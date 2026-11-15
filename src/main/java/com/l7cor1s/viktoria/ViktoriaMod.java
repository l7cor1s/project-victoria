package com.l7cor1s.viktoria;

import com.l7cor1s.viktoria.config.VikaConfig;
import com.l7cor1s.viktoria.dialogue.DialogueManager;
import com.l7cor1s.viktoria.gift.GiftTable;
import com.l7cor1s.viktoria.platform.ClientBridge;
import com.l7cor1s.viktoria.platform.Platform;
import com.l7cor1s.viktoria.skin.ServerAssetStore;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;

/**
 * My Girlfriend Laura: common entry point shared by every loader.
 * <p>
 * Loader glue calls {@link #init(Platform)} as early as possible, then registers content through
 * {@link com.l7cor1s.viktoria.registry.VikaRegistries}.
 *
 * @author vyrriox
 */
public final class ViktoriaMod {
    public static final String MODID = ViktoriaCore.MODID;
    public static final String NAME = ViktoriaCore.NAME;

    private static Platform platform;
    private static volatile ClientBridge clientBridge = ClientBridge.NOOP;

    private ViktoriaMod() {
    }

    public static void init(Platform loader) {
        platform = loader;
        ViktoriaCore.LOGGER.info("{} {} starting on {}", NAME, loader.modVersion(), loader.loaderName());
        VikaConfig.load();
        ServerAssetStore.init();
    }

    /**
     * Reloads every server-side file from the config folder. Called when a server starts and by
     * {@code /laura reload}. Item-based files are read here, after registries are frozen.
     */
    public static void reloadAll() {
        reload("viktoria-common.json", VikaConfig::load);
        reload("gifts.json", GiftTable::load);
        reload("desires.json", com.l7cor1s.viktoria.desire.DesireTable::load);
        reload("recipes.json", com.l7cor1s.viktoria.cooking.Cookbook::load);
        reload("the cooking cache", com.l7cor1s.viktoria.cooking.CookingSupport::clearCache);
        reload("the dialogues", DialogueManager::reload);
        reload("the skin and model folders", ServerAssetStore::rescan);
    }

    /**
     * One file (or a script entry added to it) with a value nobody expected must not stop the others
     * from loading, the server from starting or a server tick: the previous content stays in use.
     */
    private static void reload(String what, Runnable loader) {
        try {
            loader.run();
        } catch (RuntimeException e) {
            ViktoriaCore.LOGGER.error("Could not load {}, keeping what was loaded before", what, e);
        }
    }

    public static Platform platform() {
        if (platform == null) {
            throw new IllegalStateException(NAME + " platform used before init");
        }
        return platform;
    }

    public static boolean isInitialized() {
        return platform != null;
    }

    public static ClientBridge client() {
        return clientBridge;
    }

    public static void setClientBridge(ClientBridge bridge) {
        clientBridge = bridge == null ? ClientBridge.NOOP : bridge;
    }

    /** Root of every file this mod reads or writes: {@code config/viktoria}. */
    public static Path configDir() {
        return platform().configDir().resolve(MODID);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
