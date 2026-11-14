package com.l7cor1s.viktoria.event;

import com.l7cor1s.viktoria.autonomy.CompanionAutonomy;
import com.l7cor1s.viktoria.character.ViktoriaCharacter;
import com.l7cor1s.viktoria.compat.IntegrationReactions;
import com.l7cor1s.viktoria.entity.CompanionEntity;
import com.l7cor1s.viktoria.quest.QuestProgress;
import com.l7cor1s.viktoria.relationship.CompanionState;
import com.l7cor1s.viktoria.relationship.ModAttachments;
import com.l7cor1s.viktoria.relationship.MoodType;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;

/** Reactions to player actions: hurt, death, kills, biome visits, soft-dep flavour. */
public final class ModEvents {
    private ModEvents() {
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }
        // Attachment copyOnDeath keeps affinity; flag a soft reaction for next talk.
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CompanionState state = player.getData(ModAttachments.COMPANION_DATA.get()).getOrCreate(ViktoriaCharacter.ID);
        state.setPendingDeathReaction(true);
        state.setMood(MoodType.LONELY);
    }

    @SubscribeEvent
    public static void onHurt(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();

        // Player hurt companion
        if (victim instanceof CompanionEntity companion
                && event.getSource().getEntity() instanceof ServerPlayer player
                && companion.getCharacterId().equals(ViktoriaCharacter.ID)) {
            CompanionState state = player.getData(ModAttachments.COMPANION_DATA.get()).getOrCreate(companion.getCharacterId());
            state.addAffinity(-10);
            state.setMood(MoodType.IRRITATION);
            state.setFollowing(false);
            companion.setFollowing(false);
            companion.setAnimState("talk");
            if (state.affinity() >= 35) {
                state.setMood(MoodType.JEALOUSY);
            }
            return;
        }

        // Companion reacts when player takes damage nearby
        if (victim instanceof ServerPlayer player && event.getNewDamage() > 0.5f) {
            CompanionEntity near = nearestCompanion(player, 16.0);
            if (near == null) {
                return;
            }
            CompanionState state = player.getData(ModAttachments.COMPANION_DATA.get()).getOrCreate(near.getCharacterId());
            state.setPendingHurtReaction(true);
            if (state.affinity() >= 20) {
                state.setMood(MoodType.AFFECTION);
                near.getLookControl().setLookAt(player);
                near.setAnimState("talk");
            }
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide) {
            return;
        }

        // Hunt quest: player kills hostile mobs
        if (dead instanceof Mob && dead instanceof Enemy
                && event.getSource().getEntity() instanceof ServerPlayer player) {
            CompanionEntity near = nearestCompanion(player, 48.0);
            if (near != null) {
                CompanionState state = player.getData(ModAttachments.COMPANION_DATA.get())
                        .getOrCreate(near.getCharacterId());
                QuestProgress.onMobKill(player, near, state);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide) {
            return;
        }
        if (player.tickCount % 40 != 0) {
            return;
        }

        IntegrationReactions.onPlayerTick(player);

        CompanionEntity near = nearestCompanion(player, 64.0);
        if (near == null) {
            return;
        }
        IntegrationReactions.maybeTownFlavour(player, near);

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Holder<Biome> biome = level.getBiome(player.blockPosition());
        String biomeId = CompanionAutonomy.biomeKey(biome);
        CompanionState state = player.getData(ModAttachments.COMPANION_DATA.get()).getOrCreate(near.getCharacterId());
        QuestProgress.onBiomeVisit(player, near, state, biomeId);

        // Iron's Spells soft hint: player holding a spellbook-like item (namespace heuristic)
        if (player.tickCount % 200 == 0 && player.getMainHandItem().getItem().builtInRegistryHolder().key()
                .location().getNamespace().contains("irons")) {
            IntegrationReactions.onSpellCastHint(player);
        }
    }

    private static CompanionEntity nearestCompanion(ServerPlayer player, double range) {
        List<CompanionEntity> list = player.level().getEntitiesOfClass(CompanionEntity.class,
                new AABB(player.blockPosition()).inflate(range));
        CompanionEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (CompanionEntity c : list) {
            double d = c.distanceToSqr(player);
            if (d < bestDist) {
                bestDist = d;
                best = c;
            }
        }
        return best;
    }
}
