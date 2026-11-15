package com.l7cor1s.viktoria.event;

import com.mojang.brigadier.CommandDispatcher;
import com.l7cor1s.viktoria.ViktoriaMod;
import com.l7cor1s.viktoria.command.VikaCommand;
import com.l7cor1s.viktoria.entity.VikaEntity;
import com.l7cor1s.viktoria.network.VikaNetwork;
import com.l7cor1s.viktoria.test.VikaSelfTest;
import com.l7cor1s.viktoria.world.VikaChat;
import com.l7cor1s.viktoria.world.VikaManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Loader-independent event handlers. Each loader forwards its own events here.
 *
 * @author vyrriox
 */
public final class VikaEvents {
    private VikaEvents() {
    }

    public static void onServerStarting(MinecraftServer server) {
        ViktoriaMod.reloadAll();
        VikaManager.clear();
        VikaNetwork.clear();
    }

    public static void onServerStarted(MinecraftServer server) {
        VikaSelfTest.onServerStarted(server);
    }

    public static void onServerStopped(MinecraftServer server) {
        VikaManager.clear();
        VikaNetwork.clear();
        VikaSelfTest.onServerStopped();
    }

    public static void onServerTick(MinecraftServer server) {
        if (com.l7cor1s.viktoria.api.ScriptData.consumeReloadRequest()) {
            // Scripts changed Laura's content: rebuild the tables with it.
            ViktoriaMod.reloadAll();
        }
        VikaManager.tick(server);
        VikaNetwork.tick(server);
        VikaSelfTest.tick(server);
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        VikaCommand.register(dispatcher);
    }

    public static void onChat(ServerPlayer player, String message) {
        VikaChat.onChat(player, message);
    }

    public static void onPlayerJoin(ServerPlayer player) {
        VikaManager.onPlayerJoin(player);
    }

    public static void onPlayerLeave(ServerPlayer player) {
        VikaManager.onPlayerLeave(player);
    }

    public static void onPlayerChangedDimension(ServerPlayer player, ServerLevel from) {
        VikaManager.onPlayerChangedDimension(player, from);
    }

    public static void onPlayerDeath(ServerPlayer player) {
        VikaManager.onPlayerDeath(player);
    }

    /**
     * An entity stops being part of a server level: its chunk unloads, or it is removed. Loaders
     * forward the event they fire when entity tracking ends (EntityLeaveLevelEvent, ENTITY_UNLOAD).
     */
    public static void onEntityUnloaded(Entity entity) {
        if (entity instanceof VikaEntity laura) {
            VikaManager.onUnloaded(laura);
        }
    }

    public static void onBlockBroken(ServerPlayer player, BlockState state) {
        VikaEntity laura = VikaManager.findNear(player, 24);
        if (laura != null) {
            laura.brain().watcher().onBlockBroken(player, state);
        }
    }

    public static void onEntityKilled(LivingEntity victim, ServerPlayer killer) {
        if (victim instanceof VikaEntity) {
            return;
        }
        VikaEntity laura = VikaManager.findNear(killer, 32);
        if (laura != null) {
            laura.brain().watcher().onKill(killer, victim);
        }
    }
}
