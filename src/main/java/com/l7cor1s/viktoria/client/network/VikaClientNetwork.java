package com.l7cor1s.viktoria.client.network;

import com.l7cor1s.viktoria.ViktoriaCore;
import com.l7cor1s.viktoria.client.ClientState;
import com.l7cor1s.viktoria.client.gui.VikaMenuScreen;
import com.l7cor1s.viktoria.client.model.ClientModels;
import com.l7cor1s.viktoria.client.skin.SkinTextures;
import com.l7cor1s.viktoria.config.VikaClientConfig;
import com.l7cor1s.viktoria.inventory.VikaInventoryMenu;
import com.l7cor1s.viktoria.network.VikaAction;
import com.l7cor1s.viktoria.network.VikaNetwork;
import com.l7cor1s.viktoria.skin.AssetCache;
import com.l7cor1s.viktoria.skin.AssetKind;
import com.l7cor1s.viktoria.skin.ServerAssetStore;
import com.l7cor1s.viktoria.util.TextCodec;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client side of the mod channel.
 *
 * @author vyrriox
 */
public final class VikaClientNetwork {
    private static final Map<String, byte[]> DOWNLOADS = new HashMap<>();
    private static final Map<String, Integer> RECEIVED = new HashMap<>();

    private VikaClientNetwork() {
    }

    /** Entry point for packets received by the client, already on the client thread. */
    public static void handle(byte[] data) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        try {
            int id = buf.readVarInt();
            Minecraft mc = Minecraft.getInstance();
            switch (id) {
                case VikaNetwork.S2C_SPEECH -> {
                    int entityId = buf.readVarInt();
                    String json = buf.readUtf(32000);
                    if (mc.level != null && VikaClientConfig.speechBubbles.get()) {
                        Component text = TextCodec.fromJson(json, mc.level.registryAccess());
                        ClientState.addBubble(entityId, text, VikaClientConfig.bubbleSeconds.getInt());
                    }
                }
                case VikaNetwork.S2C_ASSET -> handleAsset(buf);
                case VikaNetwork.S2C_ASSET_LIST -> {
                    AssetKind kind = AssetKind.byId(buf.readVarInt());
                    int count = buf.readVarInt();
                    List<String[]> entries = new ArrayList<>();
                    for (int i = 0; i < count; i++) {
                        entries.add(new String[]{buf.readUtf(256), buf.readUtf(64)});
                    }
                    if (kind != null) {
                        ClientState.setAssetList(kind, entries);
                        if (mc.screen instanceof VikaMenuScreen menu) {
                            menu.onAssetList();
                        }
                    }
                }
                case VikaNetwork.S2C_SETTINGS -> ClientState.applySettings(buf.readUtf(32000));
                case VikaNetwork.S2C_MENU_CONTEXT -> {
                    int entityId = buf.readVarInt();
                    int rows = buf.readVarInt();
                    VikaInventoryMenu.setPendingClientRows(rows, entityId);
                }
                case VikaNetwork.S2C_STATUS -> {
                    int entityId = buf.readVarInt();
                    ClientState.putStatus(entityId, buf.readUtf(32000));
                    if (mc.screen instanceof VikaMenuScreen menu) {
                        menu.onStatus(entityId);
                    }
                }
                case VikaNetwork.S2C_UPLOAD_RESULT -> {
                    boolean ok = buf.readBoolean();
                    String json = buf.readUtf(32000);
                    if (mc.level != null) {
                        ClientState.lastUploadSuccess = ok;
                        ClientState.lastUploadMessage = TextCodec.fromJson(json, mc.level.registryAccess());
                        if (ok) {
                            requestList(AssetKind.SKIN);
                            requestList(AssetKind.MODEL);
                        }
                    }
                }
                default -> ViktoriaCore.LOGGER.debug("Unknown Laura packet {}", id);
            }
        } catch (RuntimeException e) {
            ViktoriaCore.LOGGER.warn("Malformed Laura packet: {}", e.toString());
        } finally {
            buf.release();
        }
    }

    private static void handleAsset(FriendlyByteBuf buf) {
        AssetKind kind = AssetKind.byId(buf.readVarInt());
        String name = buf.readUtf(256);
        String sha1 = buf.readUtf(64);
        int total = buf.readVarInt();
        int offset = buf.readVarInt();
        byte[] chunk = buf.readByteArray(VikaNetwork.DOWNLOAD_CHUNK + 16);
        if (kind == null || !ServerAssetStore.isSafeName(name)) {
            return;
        }
        String key = kind.name() + "|" + name;
        // Only files this client asked for: a server cannot push files of its own choice.
        boolean expected = kind == AssetKind.SKIN ? SkinTextures.isWaitingFor(name) : ClientModels.isWaitingFor(name);
        if (!expected) {
            DOWNLOADS.remove(key);
            RECEIVED.remove(key);
            return;
        }
        int limit = (kind == AssetKind.SKIN ? VikaClientConfig.maxSkinDownloadKb.getInt() : VikaClientConfig.maxModelDownloadKb.getInt()) * 1024;
        // The hash names the cached file: anything that is not a SHA-1 is refused like a missing file.
        if (total <= 0 || total > limit || !AssetCache.isSha1(sha1)) {
            DOWNLOADS.remove(key);
            RECEIVED.remove(key);
            if (kind == AssetKind.SKIN) {
                SkinTextures.onServerSkinMissing(name);
            } else {
                ClientModels.onServerModelMissing(name);
            }
            return;
        }
        byte[] data = DOWNLOADS.get(key);
        if (offset == 0 || data == null || data.length != total) {
            data = new byte[total];
            DOWNLOADS.put(key, data);
            RECEIVED.put(key, 0);
        }
        if (offset < 0 || offset + chunk.length > total) {
            DOWNLOADS.remove(key);
            return;
        }
        System.arraycopy(chunk, 0, data, offset, chunk.length);
        int received = RECEIVED.getOrDefault(key, 0) + chunk.length;
        RECEIVED.put(key, received);
        if (received >= total) {
            DOWNLOADS.remove(key);
            RECEIVED.remove(key);
            if (kind == AssetKind.SKIN) {
                SkinTextures.onServerSkinReceived(name, sha1, data);
            } else {
                ClientModels.onServerModelReceived(name, sha1, data);
            }
        }
    }

    // ------------------------------------------------------------------ sending

    public static void action(int entityId, VikaAction action, String arg) {
        FriendlyByteBuf buf = VikaNetwork.buffer(VikaNetwork.C2S_ACTION);
        buf.writeVarInt(entityId);
        buf.writeVarInt(action.ordinal());
        buf.writeUtf(arg == null ? "" : arg, 256);
        VikaNetwork.sendToServer(buf);
    }

    public static void setSkin(int entityId, String ref, boolean slim) {
        FriendlyByteBuf buf = VikaNetwork.buffer(VikaNetwork.C2S_SET_SKIN);
        buf.writeVarInt(entityId);
        buf.writeUtf(ref, 2048);
        buf.writeBoolean(slim);
        VikaNetwork.sendToServer(buf);
    }

    public static void setModel(int entityId, String model) {
        FriendlyByteBuf buf = VikaNetwork.buffer(VikaNetwork.C2S_SET_MODEL);
        buf.writeVarInt(entityId);
        buf.writeUtf(model, 256);
        VikaNetwork.sendToServer(buf);
    }

    public static void requestAsset(AssetKind kind, String name) {
        FriendlyByteBuf buf = VikaNetwork.buffer(VikaNetwork.C2S_REQUEST_ASSET);
        buf.writeVarInt(kind.ordinal());
        buf.writeUtf(name, 256);
        VikaNetwork.sendToServer(buf);
    }

    public static void requestList(AssetKind kind) {
        FriendlyByteBuf buf = VikaNetwork.buffer(VikaNetwork.C2S_REQUEST_LIST);
        buf.writeVarInt(kind.ordinal());
        VikaNetwork.sendToServer(buf);
    }

    public static void requestStatus(int entityId) {
        FriendlyByteBuf buf = VikaNetwork.buffer(VikaNetwork.C2S_REQUEST_STATUS);
        buf.writeVarInt(entityId);
        VikaNetwork.sendToServer(buf);
    }

    public static void hello() {
        VikaNetwork.sendToServer(VikaNetwork.buffer(VikaNetwork.C2S_HELLO));
    }

    /** Uploads a file in chunks under the vanilla size limit. */
    public static void upload(AssetKind kind, String name, byte[] data, int entityId, boolean slim) {
        for (int offset = 0; offset < data.length; offset += VikaNetwork.UPLOAD_CHUNK) {
            int len = Math.min(VikaNetwork.UPLOAD_CHUNK, data.length - offset);
            byte[] chunk = new byte[len];
            System.arraycopy(data, offset, chunk, 0, len);
            FriendlyByteBuf buf = VikaNetwork.buffer(VikaNetwork.C2S_UPLOAD);
            buf.writeVarInt(kind.ordinal());
            buf.writeUtf(name, 128);
            buf.writeVarInt(data.length);
            buf.writeVarInt(offset);
            buf.writeByteArray(chunk);
            buf.writeVarInt(entityId);
            buf.writeBoolean(slim);
            VikaNetwork.sendToServer(buf);
        }
    }

    public static void reset() {
        DOWNLOADS.clear();
        RECEIVED.clear();
    }
}
