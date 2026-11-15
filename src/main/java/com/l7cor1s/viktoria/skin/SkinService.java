package com.l7cor1s.viktoria.skin;

import com.l7cor1s.viktoria.world.VikaAdvancements;
import com.l7cor1s.viktoria.ViktoriaCore;
import com.l7cor1s.viktoria.config.VikaConfig;
import com.l7cor1s.viktoria.entity.VikaEntity;
import com.l7cor1s.viktoria.model.ModelParser;
import com.l7cor1s.viktoria.network.VikaNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Server-side validation and application of skin and model changes.
 *
 * @author vyrriox
 */
public final class SkinService {
    private SkinService() {
    }

    private static long lastRescan;

    /** Rescans the asset folders at most every 5 seconds, so unknown names cannot be used to spam disk scans. */
    private static void rescanThrottled() {
        long now = System.currentTimeMillis();
        if (now - lastRescan > 5000) {
            lastRescan = now;
            ServerAssetStore.rescan();
        }
    }

    /**
     * Gives her the default skin of the config. A server file gets its current hash (clients
     * download it by hash), a player name is looked up in the background (she wears the built-in
     * skin until the answer arrives), anything unusable falls back to the built-in skin.
     */
    public static void applyDefaultSkin(VikaEntity laura) {
        SkinRef ref = SkinRef.parse(VikaConfig.defaultSkin.get());
        switch (ref.type()) {
            case SERVER -> {
                ServerAssetStore.Entry entry = entry(AssetKind.SKIN, ref.value());
                laura.setSkin(entry == null ? SkinRef.DEFAULT : new SkinRef(SkinRef.Type.SERVER, entry.name(), entry.sha1()), true);
            }
            case PLAYER -> {
                laura.setSkin(SkinRef.DEFAULT, true);
                MinecraftServer server = laura.getServer();
                if (server == null || !VikaConfig.allowPlayerNameSkins.get() || !MojangSkinResolver.isValidName(ref.value())) {
                    return;
                }
                MojangSkinResolver.resolve(ref.value()).thenAccept(result -> server.execute(() -> {
                    // Only if she still wears the placeholder: a skin chosen in the meantime stays.
                    if (laura.isAlive() && result.isPresent() && laura.getSkin().equals(SkinRef.DEFAULT)) {
                        MojangSkinResolver.Result r = result.get();
                        laura.setSkin(new SkinRef(SkinRef.Type.URL, r.url(), ""), r.slim());
                        laura.setSkinLabel("player:" + r.name());
                    }
                }));
            }
            case URL -> {
                String url = SkinRef.normalizeUrl(ref.value());
                boolean allowed = VikaConfig.allowUrlSkins.get() && url.length() <= 1024 && SkinRef.isAllowedUrl(url, VikaConfig.urlDomainWhitelist.get());
                laura.setSkin(allowed ? new SkinRef(SkinRef.Type.URL, url, "") : SkinRef.DEFAULT, true);
            }
            default -> laura.setSkin(ref, true);
        }
    }

    /**
     * Always applies the embedded Vika model shipped in the mod jar ({@code pack:Vika}).
     * Extra models in {@code config/projectvika/models/uploads/} stay available for players to pick later.
     */
    public static void applyDefaultModel(VikaEntity laura) {
        laura.setModel("pack:Vika");
        ViktoriaCore.LOGGER.info("Applied embedded default model pack:Vika to {}", laura.getUUID());
    }

    /** A server file by name, looking again in the folder if it was added since the last scan. */
    private static ServerAssetStore.Entry entry(AssetKind kind, String name) {
        ServerAssetStore.Entry entry = ServerAssetStore.get(kind, name);
        if (entry == null && ServerAssetStore.isSafeName(name)) {
            rescanThrottled();
            entry = ServerAssetStore.get(kind, name);
        }
        return entry;
    }

    public static boolean canChangeLook(ServerPlayer player, VikaEntity laura) {
        return laura.isOwnedBy(player) || VikaConfig.othersCanChangeSkin.get() || player.hasPermissions(2);
    }

    /** Seconds between two skin changes, and between two model changes, of the same companion. */
    private static final double LOOK_CHANGE_SECONDS = 2;
    /** One account lookup every few seconds per player: each one is a request to the Mojang API. */
    private static final long PLAYER_LOOKUP_INTERVAL_MS = 5000;
    private static final java.util.Map<java.util.UUID, Long> PLAYER_LOOKUPS = new java.util.HashMap<>();

    /**
     * Every client near her downloads and keeps a texture for each new look: a companion changes
     * skin (or model) at most once every few seconds. Returns false, with a message, when it is too soon.
     */
    private static boolean lookReady(ServerPlayer player, VikaEntity laura, String what) {
        if (laura.brain().readyFixed("look." + what, LOOK_CHANGE_SECONDS)) {
            return true;
        }
        player.sendSystemMessage(Component.translatable("viktoria.look.cooldown"));
        return false;
    }

    /** Forgets what was kept about a player who left. */
    public static void forget(ServerPlayer player) {
        PLAYER_LOOKUPS.remove(player.getUUID());
    }

    /** Applies a skin reference sent by a client or typed in a command. Returns a feedback message. */
    public static void requestSkin(ServerPlayer player, VikaEntity laura, String rawRef, boolean slim) {
        if (!canChangeLook(player, laura)) {
            player.sendSystemMessage(Component.translatable("viktoria.skin.not_allowed"));
            return;
        }
        if (!lookReady(player, laura, "skin")) {
            return;
        }
        String raw = rawRef == null ? "" : rawRef.trim();
        if (raw.equalsIgnoreCase("reset") || raw.isEmpty()) {
            applyDefaultSkin(laura);
            player.sendSystemMessage(Component.translatable("viktoria.skin.reset"));
            return;
        }
        SkinRef ref = SkinRef.parse(raw.startsWith("http") ? "url:" + raw : raw);
        switch (ref.type()) {
            case BUILTIN -> {
                laura.setSkin(ref, slim);
                player.sendSystemMessage(Component.translatable("viktoria.skin.changed", Component.translatable("viktoria.skin.builtin." + ref.value())));
                VikaAdvancements.award(player, "new_look");
            }
            case URL -> {
                if (!VikaConfig.allowUrlSkins.get()) {
                    player.sendSystemMessage(Component.translatable("viktoria.skin.url_disabled"));
                    return;
                }
                String url = SkinRef.normalizeUrl(ref.value());
                if (url.length() > 1024 || !SkinRef.isAllowedUrl(url, VikaConfig.urlDomainWhitelist.get())) {
                    player.sendSystemMessage(Component.translatable("viktoria.skin.url_refused", String.join(", ", VikaConfig.urlDomainWhitelist.get())));
                    return;
                }
                laura.setSkin(new SkinRef(SkinRef.Type.URL, url, ""), slim);
                player.sendSystemMessage(Component.translatable("viktoria.skin.changed", Component.literal(url)));
                VikaAdvancements.award(player, "new_look");
            }
            case SERVER -> {
                ServerAssetStore.Entry entry = ServerAssetStore.get(AssetKind.SKIN, ref.value());
                if (entry == null) {
                    // A file added to the folder since the last scan.
                    rescanThrottled();
                    entry = ServerAssetStore.get(AssetKind.SKIN, ref.value());
                }
                if (entry == null) {
                    player.sendSystemMessage(Component.translatable("viktoria.skin.file_missing", ref.value()));
                    return;
                }
                laura.setSkin(new SkinRef(SkinRef.Type.SERVER, entry.name(), entry.sha1()), slim);
                player.sendSystemMessage(Component.translatable("viktoria.skin.changed", Component.literal(entry.name())));
                VikaAdvancements.award(player, "new_look");
            }
            case PLAYER -> {
                if (!VikaConfig.allowPlayerNameSkins.get()) {
                    player.sendSystemMessage(Component.translatable("viktoria.skin.player_disabled"));
                    return;
                }
                if (!MojangSkinResolver.isValidName(ref.value())) {
                    player.sendSystemMessage(Component.translatable("viktoria.skin.player_invalid", ref.value()));
                    return;
                }
                long nowMs = System.currentTimeMillis();
                Long lastLookup = PLAYER_LOOKUPS.get(player.getUUID());
                if (lastLookup != null && nowMs - lastLookup < PLAYER_LOOKUP_INTERVAL_MS || MojangSkinResolver.isBusy(ref.value())) {
                    player.sendSystemMessage(Component.translatable("viktoria.look.cooldown"));
                    return;
                }
                PLAYER_LOOKUPS.put(player.getUUID(), nowMs);
                player.sendSystemMessage(Component.translatable("viktoria.skin.player_searching", ref.value()));
                MinecraftServer server = player.getServer();
                MojangSkinResolver.resolve(ref.value()).thenAccept(result -> {
                    if (server == null) {
                        return;
                    }
                    server.execute(() -> {
                        if (!laura.isAlive()) {
                            return;
                        }
                        if (result.isEmpty()) {
                            player.sendSystemMessage(Component.translatable("viktoria.skin.player_not_found", ref.value()));
                            return;
                        }
                        MojangSkinResolver.Result r = result.get();
                        laura.setSkin(new SkinRef(SkinRef.Type.URL, r.url(), ""), r.slim());
                        laura.setSkinLabel("player:" + r.name());
                        player.sendSystemMessage(Component.translatable("viktoria.skin.changed", Component.literal(r.name())));
                        VikaAdvancements.award(player, "new_look");
                    });
                });
            }
        }
    }

    public static void requestModel(ServerPlayer player, VikaEntity laura, String rawName) {
        if (!canChangeLook(player, laura)) {
            player.sendSystemMessage(Component.translatable("viktoria.skin.not_allowed"));
            return;
        }
        if (!lookReady(player, laura, "model")) {
            return;
        }
        String name = rawName == null ? "" : rawName.trim();
        if (name.isEmpty() || name.equalsIgnoreCase("reset") || name.equalsIgnoreCase("default")) {
            applyDefaultModel(laura);
            player.sendSystemMessage(Component.translatable("viktoria.model.reset"));
            return;
        }
        if (!VikaConfig.allowCustomModels.get()) {
            player.sendSystemMessage(Component.translatable("viktoria.model.disabled"));
            return;
        }
        if (name.startsWith("pack:")) {
            String packName = name.substring(5);
            if (!ServerAssetStore.isSafeName(packName)) {
                return;
            }
            laura.setModel("pack:" + packName);
            player.sendSystemMessage(Component.translatable("viktoria.model.changed", packName));
            VikaAdvancements.award(player, "makeover");
            return;
        }
        String serverName = name.startsWith("server:") ? name.substring(7) : name;
        int hash = serverName.indexOf('#');
        if (hash > 0) {
            serverName = serverName.substring(0, hash);
        }
        ServerAssetStore.Entry entry = ServerAssetStore.get(AssetKind.MODEL, serverName);
        if (entry == null && ServerAssetStore.isSafeName(serverName)) {
            // A file added to the folder since the last scan.
            rescanThrottled();
            entry = ServerAssetStore.get(AssetKind.MODEL, serverName);
        }
        if (entry == null) {
            // Not on the server: maybe a resource pack model the clients have.
            if (ServerAssetStore.isSafeName(serverName)) {
                laura.setModel("pack:" + serverName);
                player.sendSystemMessage(Component.translatable("viktoria.model.changed_pack", serverName));
                VikaAdvancements.award(player, "makeover");
            }
            return;
        }
        laura.setModel("server:" + entry.name() + "#" + entry.sha1());
        player.sendSystemMessage(Component.translatable("viktoria.model.changed", entry.name()));
        VikaAdvancements.award(player, "makeover");
    }

    /** Called when a complete upload arrived. */
    /** Files one player may keep in the uploads folder (replacing one of their own files is always allowed). */
    public static int uploadQuota(AssetKind kind) {
        return kind == AssetKind.SKIN ? VikaConfig.maxSkinUploadsPerPlayer.getInt() : VikaConfig.maxModelUploadsPerPlayer.getInt();
    }

    /**
     * The start of every file a player uploads: their UUID without dashes. It has a fixed length, so
     * it is never the start of another player's prefix (a name could be: "bob_" and "bob_ross_"),
     * and it does not change when the account is renamed.
     */
    public static String uploadPrefix(ServerPlayer player) {
        return player.getUUID().toString().replace("-", "") + "_";
    }

    public static boolean hasUploadRoom(ServerPlayer player, AssetKind kind, String name) {
        String prefix = uploadPrefix(player);
        return ServerAssetStore.hasUpload(kind, prefix + sanitize(name)) || ServerAssetStore.countUploads(kind, prefix) < uploadQuota(kind);
    }

    public static void finishUpload(ServerPlayer player, VikaEntity laura, AssetKind kind, String name, byte[] data, boolean slim) {
        if (laura == null || !canChangeLook(player, laura)) {
            VikaNetwork.uploadResult(player, false, Component.translatable("viktoria.skin.not_allowed"));
            return;
        }
        if (!hasUploadRoom(player, kind, name)) {
            VikaNetwork.uploadResult(player, false, Component.translatable("viktoria.upload.quota", uploadQuota(kind)));
            return;
        }
        String stem = uploadPrefix(player) + sanitize(name);
        if (kind == AssetKind.SKIN) {
            if (!ServerAssetStore.isValidSkinPng(data)) {
                VikaNetwork.uploadResult(player, false, Component.translatable("viktoria.upload.invalid_skin"));
                return;
            }
        } else {
            if (name.toLowerCase(Locale.ROOT).endsWith(".geo.json")) {
                // Stored uploads are single .bbmodel files (they hold their textures).
                VikaNetwork.uploadResult(player, false, Component.translatable("viktoria.upload.bbmodel_only"));
                return;
            }
            try {
                // Whatever the parser throws (a limit, a stack exhausted by a deeply nested file, a lack
                // of memory), the file is refused like any other bad file.
                ModelParser.parseChecked(name.toLowerCase(Locale.ROOT).endsWith(".bbmodel") ? name : name + ".bbmodel", new String(data, StandardCharsets.UTF_8), null);
            } catch (ModelParser.InvalidModelException e) {
                if (e.getCause() instanceof Error) {
                    // Not a mistake in a model: a file made to break the parser.
                    ViktoriaCore.LOGGER.warn("Model upload {} from {} refused: {}", sanitize(name), player.getGameProfile().getName(), e.getCause().toString());
                }
                VikaNetwork.uploadResult(player, false, Component.translatable("viktoria.upload.invalid_model", String.valueOf(e.getMessage())));
                return;
            }
        }
        try {
            ServerAssetStore.Entry entry = ServerAssetStore.storeUpload(kind, stem, data);
            if (entry == null) {
                VikaNetwork.uploadResult(player, false, Component.translatable("viktoria.upload.failed"));
                return;
            }
            ViktoriaCore.LOGGER.info("{} uploaded {} {}", player.getGameProfile().getName(), kind.name().toLowerCase(Locale.ROOT), entry.name());
            VikaNetwork.uploadResult(player, true, Component.translatable("viktoria.upload.done", entry.name()));
            if (kind == AssetKind.SKIN) {
                laura.setSkin(new SkinRef(SkinRef.Type.SERVER, entry.name(), entry.sha1()), slim);
                VikaAdvancements.award(player, "new_look");
            } else {
                laura.setModel("server:" + entry.name() + "#" + entry.sha1());
                VikaAdvancements.award(player, "makeover");
            }
        } catch (IOException e) {
            ViktoriaCore.LOGGER.warn("Upload from {} failed: {}", player.getGameProfile().getName(), e.getMessage());
            VikaNetwork.uploadResult(player, false, Component.translatable("viktoria.upload.failed"));
        }
    }

    private static String sanitize(String s) {
        StringBuilder sb = new StringBuilder();
        String stem = s.replaceAll("(?i)\\.(png|bbmodel|geo\\.json)$", "");
        for (char c : stem.toLowerCase(Locale.ROOT).toCharArray()) {
            if (c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == '-') {
                sb.append(c);
            }
            if (sb.length() >= 32) {
                break;
            }
        }
        return sb.isEmpty() ? "file" : sb.toString();
    }
}
