package com.l7cor1s.viktoria.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.l7cor1s.viktoria.ViktoriaCore;
import com.l7cor1s.viktoria.ViktoriaMod;
import com.l7cor1s.viktoria.client.anim.DefaultAnimations;
import com.l7cor1s.viktoria.client.gui.EmoteWheelScreen;
import com.l7cor1s.viktoria.client.gui.VikaMenuScreen;
import com.l7cor1s.viktoria.client.lang.LanguageOverlay;
import com.l7cor1s.viktoria.client.model.ClientModels;
import com.l7cor1s.viktoria.client.network.VikaClientNetwork;
import com.l7cor1s.viktoria.client.skin.SkinTextures;
import com.l7cor1s.viktoria.config.VikaClientConfig;
import com.l7cor1s.viktoria.desire.DesireTable;
import com.l7cor1s.viktoria.entity.VikaEntity;
import com.l7cor1s.viktoria.platform.ClientBridge;
import com.l7cor1s.viktoria.util.CacheFiles;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;

/**
 * Client entry point shared by every loader.
 *
 * @author vyrriox
 */
public final class VikaClient {
    public static final String KEY_CATEGORY = "key.categories.viktoria";
    public static final KeyMapping MENU_KEY = new KeyMapping("key.viktoria.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, KEY_CATEGORY);
    public static final KeyMapping EMOTE_KEY = new KeyMapping("key.viktoria.emotes", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, KEY_CATEGORY);
    public static final KeyMapping CALL_KEY = new KeyMapping("key.viktoria.call", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), KEY_CATEGORY);

    private static boolean joined;
    /** Ticks to wait for the server to show it has the mod before leaving it. */
    private static final int SERVER_CHECK_TICKS = 100;
    /** Client ticks since the world was joined, -1 once the server check is done. */
    private static int ticksSinceJoin = -1;

    private VikaClient() {
    }

    public static List<KeyMapping> keyMappings() {
        return List.of(MENU_KEY, EMOTE_KEY, CALL_KEY);
    }

    /** Called once by the loader during client setup. */
    public static void init() {
        VikaClientConfig.load();
        DefaultAnimations.reload();
        pruneDownloadCache();
        ViktoriaMod.setClientBridge(new ClientBridge() {
            @Override
            public void openInteractionScreen(VikaEntity laura) {
                Minecraft mc = Minecraft.getInstance();
                if (!VikaClientConfig.menuOnRightClick.get() || mc.player == null) {
                    return;
                }
                if (!laura.isOwnedBy(mc.player) && !ClientState.othersCanInteract) {
                    return;
                }
                mc.execute(() -> mc.setScreen(new VikaMenuScreen(laura)));
            }

            @Override
            public boolean isScreenOpen() {
                return Minecraft.getInstance().screen instanceof VikaMenuScreen;
            }

            @Override
            public void handlePacket(byte[] data) {
                VikaClientNetwork.handle(data);
            }
        });
    }

    /** Downloaded skins and models are kept 30 days after their last use, within a size limit per folder. */
    private static void pruneDownloadCache() {
        java.nio.file.Path cache = ViktoriaMod.platform().gameDir().resolve("viktoria").resolve("cache");
        Thread pruner = new Thread(() -> {
            long maxAge = 30L * 24 * 60 * 60 * 1000;
            int skins = CacheFiles.prune(cache.resolve("skins"), maxAge, 64L * 1024 * 1024);
            int models = CacheFiles.prune(cache.resolve("models"), maxAge, 256L * 1024 * 1024);
            if (skins + models > 0) {
                ViktoriaCore.LOGGER.info("Removed {} old skins and {} old models from the download cache", skins, models);
            }
        }, "Laura cache cleanup");
        pruner.setDaemon(true);
        pruner.start();
    }

    /** Called after the registries are ready (item icons in desires need them). */
    public static void afterRegistries() {
        DesireTable.load();
    }

    /** Called every client tick (end phase). */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientState.ticks++;
        ClientSelfTest.tick();
        if (mc.player == null || mc.level == null) {
            if (joined) {
                joined = false;
                onDisconnect();
            }
            return;
        }
        if (!joined) {
            joined = true;
            ticksSinceJoin = 0;
            VikaClientNetwork.hello();
        }
        // The mod is needed on both sides. NeoForge and Forge refuse such a connection themselves;
        // where the loader lets it through, the player leaves with a clear message instead of
        // playing with a mod that cannot work. The server answers the hello with its settings: no
        // answer and no channel after a few seconds means it does not have the mod.
        if (ticksSinceJoin >= 0 && ++ticksSinceJoin > SERVER_CHECK_TICKS) {
            ticksSinceJoin = -1;
            if (!ClientState.serverKnown && !ViktoriaMod.platform().serverHasChannel()) {
                ViktoriaCore.LOGGER.warn("This server does not have {}: leaving", ViktoriaMod.NAME);
                mc.player.connection.getConnection().disconnect(Component.translatable("viktoria.network.server_missing"));
                return;
            }
        }
        LanguageOverlay.ensureInstalled();
        while (MENU_KEY.consumeClick()) {
            VikaEntity laura = nearestOwned(64);
            if (laura != null) {
                mc.setScreen(new VikaMenuScreen(laura));
            } else {
                mc.player.displayClientMessage(Component.translatable("viktoria.menu.none_near"), true);
            }
        }
        while (EMOTE_KEY.consumeClick()) {
            VikaEntity laura = nearestOwned(32);
            if (laura != null && mc.screen == null) {
                mc.setScreen(new EmoteWheelScreen(laura));
            }
        }
        while (CALL_KEY.consumeClick()) {
            mc.player.connection.sendCommand("laura come");
        }
    }

    /** Owned companions near the local player, nearest first. */
    public static List<VikaEntity> ownedNearby(double range) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return List.of();
        }
        return mc.level.getEntitiesOfClass(VikaEntity.class, mc.player.getBoundingBox().inflate(range),
                        l -> l.isAlive() && (l.isOwnedBy(mc.player) || ClientState.othersCanInteract))
                .stream().sorted(Comparator.comparingDouble(l -> l.distanceToSqr(mc.player))).toList();
    }

    public static VikaEntity nearestOwned(double range) {
        List<VikaEntity> list = ownedNearby(range);
        return list.isEmpty() ? null : list.get(0);
    }

    private static void onDisconnect() {
        ClientState.reset();
        VikaClientNetwork.reset();
        SkinTextures.clear();
        ClientModels.clear();
    }

    /** Called after a client resource reload. */
    public static void onResourceReload() {
        DefaultAnimations.reload();
        ClientModels.clear();
        LanguageOverlay.reload();
    }
}
