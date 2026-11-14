package com.l7cor1s.viktoria.gift;

import com.l7cor1s.viktoria.character.CompanionCharacter;
import com.l7cor1s.viktoria.compat.IntegrationReactions;
import com.l7cor1s.viktoria.entity.CompanionEntity;
import com.l7cor1s.viktoria.quest.QuestProgress;
import com.l7cor1s.viktoria.relationship.CompanionState;
import com.l7cor1s.viktoria.relationship.ModAttachments;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

public final class GiftHandler {
    private GiftHandler() {
    }

    public static InteractionResult tryGiveGift(ServerPlayer player, CompanionEntity companion, ItemStack stack) {
        if (stack.isEmpty()) {
            return InteractionResult.PASS;
        }

        CompanionCharacter character = companion.character();
        CompanionState state = player.getData(ModAttachments.COMPANION_DATA.get()).getOrCreate(character.id());
        GiftProfile profile = character.resolvedGiftProfile();

        int delta = character.giftAffinityDelta(stack);
        state.addAffinity(delta);
        state.setMood(character.giftMoodDelta(stack));
        long now = player.level().getGameTime();
        state.touch(now);
        state.rememberGift(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), now);

        companion.setAnimState(delta >= 0 ? "accept_gift" : "talk");
        companion.getLookControl().setLookAt(player);

        character.reactToGift(stack, state.affinity()).ifPresent(key ->
                player.displayClientMessage(Component.translatable(key, character.displayName(),
                        state.tier().label(), state.mood().label()), false));

        if (IntegrationReactions.isSpecialFood(stack)) {
            IntegrationReactions.reactSpecialFood(player, companion, state);
        }

        QuestProgress.onGift(player, companion, state, stack);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }
}
