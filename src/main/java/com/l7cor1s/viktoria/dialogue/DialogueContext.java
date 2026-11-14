package com.l7cor1s.viktoria.dialogue;

import com.l7cor1s.viktoria.entity.CompanionEntity;
import com.l7cor1s.viktoria.relationship.CompanionState;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

/** Picks contextual greeting / memory nodes from world + relationship state. */
public final class DialogueContext {
    private DialogueContext() {
    }

    public static String resolveGreeting(ServerPlayer player, CompanionEntity companion, CompanionState state) {
        long now = player.level().getGameTime();

        if (state.pendingDeathReaction()) {
            state.setPendingDeathReaction(false);
            return "memory_death";
        }
        if (state.pendingHurtReaction()) {
            state.setPendingHurtReaction(false);
            return "memory_hurt";
        }

        if (state.lastInteractGameTime() > 0 && now - state.lastInteractGameTime() > 24000L) {
            return "memory_missed";
        }

        if (!state.lastGiftId().isBlank() && now - state.lastGiftGameTime() < 24000L) {
            if (isFlowerId(state.lastGiftId())) {
                return "memory_gift_flower";
            }
            return "memory_gift";
        }

        if (player.level() instanceof ServerLevel level) {
            if (level.isRaining()) {
                return "ambient_rain";
            }
            long t = level.getDayTime() % 24000L;
            if (t >= 11000L && t < 13000L) {
                return "ambient_sunset";
            }
            if (t >= 13000L || t < 1000L) {
                return "ambient_night";
            }
            Holder<Biome> biome = level.getBiome(companion.blockPosition());
            String key = biome.unwrapKey().map(k -> k.location().getPath()).orElse("");
            if (biome.is(BiomeTags.IS_FOREST) || key.contains("forest")) {
                return "ambient_forest";
            }
            if (key.contains("desert") || key.contains("badlands")) {
                return "ambient_desert";
            }
            if (key.contains("snow") || key.contains("frozen") || key.contains("ice")) {
                return "ambient_snow";
            }
        }

        return "greeting";
    }

    private static boolean isFlowerId(String itemId) {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null) {
            return itemId.contains("poppy") || itemId.contains("rose") || itemId.contains("tulip")
                    || itemId.contains("orchid") || itemId.contains("lilac") || itemId.contains("petals");
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        return item != Items.AIR && new net.minecraft.world.item.ItemStack(item).is(ItemTags.FLOWERS);
    }
}
