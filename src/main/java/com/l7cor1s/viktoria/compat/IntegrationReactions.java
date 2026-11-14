package com.l7cor1s.viktoria.compat;

import com.l7cor1s.viktoria.entity.CompanionEntity;
import com.l7cor1s.viktoria.relationship.CompanionState;
import com.l7cor1s.viktoria.relationship.MoodType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Flavour reactions when soft-dep mods are present. Safe no-ops otherwise. */
public final class IntegrationReactions {
    private IntegrationReactions() {
    }

    public static void onPlayerTick(ServerPlayer player) {
        if (player.tickCount % 80 != 0) {
            return;
        }
        CompanionEntity nearby = findNearby(player, 10.0);
        if (nearby == null) {
            return;
        }
        if (ModCompat.create() && player.tickCount % 400 == 0) {
            int machines = countNearbyBlockEntities(player.level(), nearby.blockPosition(), 4);
            if (machines >= 4) {
                player.displayClientMessage(Component.translatable("viktoria.compat.create.comment",
                        nearby.character().displayName()), true);
            }
        }
    }

    public static void onSpellCastHint(ServerPlayer player) {
        if (!ModCompat.ironsSpells()) {
            return;
        }
        CompanionEntity nearby = findNearby(player, 12.0);
        if (nearby == null) {
            return;
        }
        player.displayClientMessage(Component.translatable("viktoria.compat.irons.cast",
                nearby.character().displayName()), true);
    }

    public static boolean isSpecialFood(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String path = id.getPath();
        String ns = id.getNamespace();
        if (ModCompat.farmersDelight() && "farmersdelight".equals(ns)) {
            return true;
        }
        if (ModCompat.foodModPresent() && (path.contains("pie") || path.contains("cake")
                || path.contains("dumpling") || path.contains("noodle") || path.contains("soup"))) {
            return true;
        }
        return false;
    }

    public static void reactSpecialFood(ServerPlayer player, CompanionEntity companion, CompanionState state) {
        state.setMood(MoodType.JOY);
        player.displayClientMessage(Component.translatable("viktoria.compat.food.delight",
                companion.character().displayName()), false);
    }

    public static void maybeTownFlavour(ServerPlayer player, CompanionEntity companion) {
        if (!ModCompat.mca() || player.tickCount % 200 != 0) {
            return;
        }
        long villagers = player.level().getEntitiesOfClass(
                net.minecraft.world.entity.npc.Villager.class,
                companion.getBoundingBox().inflate(16.0)).size();
        if (villagers >= 2 && player.getRandom().nextFloat() < 0.08f) {
            player.displayClientMessage(Component.translatable("viktoria.compat.mca.town",
                    companion.character().displayName()), true);
        }
    }

    private static int countNearbyBlockEntities(Level level, BlockPos origin, int radius) {
        int count = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    BlockEntity be = level.getBlockEntity(cursor);
                    if (be != null) {
                        count++;
                        if (count >= 8) {
                            return count;
                        }
                    }
                }
            }
        }
        return count;
    }

    private static CompanionEntity findNearby(ServerPlayer player, double range) {
        Level level = player.level();
        List<CompanionEntity> list = level.getEntitiesOfClass(CompanionEntity.class,
                new AABB(player.blockPosition()).inflate(range));
        return list.isEmpty() ? null : list.getFirst();
    }
}
