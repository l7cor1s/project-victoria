package com.l7cor1s.viktoria.command;

import com.l7cor1s.viktoria.character.CharacterRegistry;
import com.l7cor1s.viktoria.character.ViktoriaCharacter;
import com.l7cor1s.viktoria.entity.CompanionEntity;
import com.l7cor1s.viktoria.entity.ModEntities;
import com.l7cor1s.viktoria.home.HomeManager;
import com.l7cor1s.viktoria.relationship.CompanionState;
import com.l7cor1s.viktoria.relationship.ModAttachments;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class ViktoriaCommands {
    private ViktoriaCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("viktoria")
                .requires(source -> source.hasPermission(0))
                .then(Commands.literal("summon").executes(ctx -> summon(ctx.getSource(), ViktoriaCharacter.ID))
                        .then(Commands.argument("character", StringArgumentType.word())
                                .executes(ctx -> summon(ctx.getSource(), StringArgumentType.getString(ctx, "character")))))
                .then(Commands.literal("spawn").executes(ctx -> summon(ctx.getSource(), ViktoriaCharacter.ID)))
                .then(Commands.literal("follow").executes(ctx -> toggleFollow(ctx.getSource())))
                .then(Commands.literal("home").executes(ctx -> claimHome(ctx.getSource())))
                .then(Commands.literal("affinity")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                .executes(ctx -> setAffinity(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "value")))))
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())));

        dispatcher.register(root);
        dispatcher.register(Commands.literal("vika").requires(source -> source.hasPermission(0))
                .then(Commands.literal("spawn").executes(ctx -> summon(ctx.getSource(), ViktoriaCharacter.ID)))
                .then(Commands.literal("summon").executes(ctx -> summon(ctx.getSource(), ViktoriaCharacter.ID)))
                .then(Commands.literal("follow").executes(ctx -> toggleFollow(ctx.getSource())))
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource()))));
    }

    private static int summon(CommandSourceStack source, String characterId) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Только игрок"));
            return 0;
        }
        if (CharacterRegistry.get(characterId).isEmpty()) {
            source.sendFailure(Component.translatable("viktoria.command.unknown_character", characterId));
            return 0;
        }
        ServerLevel level = player.serverLevel();
        CompanionEntity entity = ModEntities.COMPANION.get().create(level);
        if (entity == null) {
            source.sendFailure(Component.literal("Не удалось создать сущность"));
            return 0;
        }
        Vec3 look = player.getLookAngle();
        entity.setCharacterId(characterId);
        entity.moveTo(player.getX() + look.x * 2, player.getY(), player.getZ() + look.z * 2,
                player.getYRot(), 0);
        CompanionState state = player.getData(ModAttachments.COMPANION_DATA.get()).getOrCreate(characterId);
        entity.setFollowing(state.following());
        level.addFreshEntity(entity);
        source.sendSuccess(() -> Component.translatable("viktoria.command.summoned",
                entity.character().displayName()), true);
        return 1;
    }

    private static int toggleFollow(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        CompanionState state = player.getData(ModAttachments.COMPANION_DATA.get()).getOrCreate(ViktoriaCharacter.ID);
        state.setFollowing(!state.following());
        player.level().getEntitiesOfClass(CompanionEntity.class, player.getBoundingBox().inflate(64),
                        e -> ViktoriaCharacter.ID.equals(e.getCharacterId()))
                .forEach(e -> e.setFollowing(state.following()));
        source.sendSuccess(() -> Component.translatable(
                state.following() ? "viktoria.follow.on" : "viktoria.follow.off",
                Component.translatable("entity.viktoria.companion.viktoria")), true);
        return 1;
    }

    private static int claimHome(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        CompanionState state = player.getData(ModAttachments.COMPANION_DATA.get()).getOrCreate(ViktoriaCharacter.ID);
        CompanionEntity nearby = player.level().getEntitiesOfClass(CompanionEntity.class, player.getBoundingBox().inflate(16),
                        e -> ViktoriaCharacter.ID.equals(e.getCharacterId()))
                .stream().findFirst().orElse(null);
        if (nearby == null) {
            source.sendFailure(Component.translatable("viktoria.command.no_nearby"));
            return 0;
        }
        HomeManager.claimHome(player, nearby, state);
        return 1;
    }

    private static int setAffinity(CommandSourceStack source, int value) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        CompanionState state = player.getData(ModAttachments.COMPANION_DATA.get()).getOrCreate(ViktoriaCharacter.ID);
        state.addAffinity(value - state.affinity());
        source.sendSuccess(() -> Component.translatable("viktoria.command.affinity_set", value, state.tier().label()), true);
        return 1;
    }

    private static int status(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        CompanionState state = player.getData(ModAttachments.COMPANION_DATA.get()).getOrCreate(ViktoriaCharacter.ID);
        source.sendSuccess(() -> Component.translatable("viktoria.status.line",
                Component.translatable("entity.viktoria.companion.viktoria"),
                state.affinity(), state.tier().label(), state.mood().label()), false);
        return 1;
    }
}
