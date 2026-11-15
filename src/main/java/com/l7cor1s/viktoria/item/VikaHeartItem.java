package com.l7cor1s.viktoria.item;

import com.l7cor1s.viktoria.dialogue.LineFormatter;
import com.l7cor1s.viktoria.entity.Emote;
import com.l7cor1s.viktoria.entity.VikaEntity;
import com.l7cor1s.viktoria.entity.VikaSpeech;
import com.l7cor1s.viktoria.entity.brain.Needs;
import com.l7cor1s.viktoria.world.VikaManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Laura's Heart: cook a pink tulip in a furnace, then use it to summon Laura. If she is already
 * with you, the heart calls her back and makes her very, very happy.
 *
 * @author vyrriox
 */
public class VikaHeartItem extends Item {
    public VikaHeartItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        VikaEntity existing = VikaManager.find(serverPlayer);
        if (existing == null && VikaManager.hasDeadCompanion(serverPlayer)) {
            player.sendSystemMessage(Component.translatable("viktoria.grave.heart_hint").withStyle(ChatFormatting.LIGHT_PURPLE));
            return InteractionResultHolder.fail(stack);
        }
        if (existing != null) {
            if (existing.level() != level || existing.distanceToSqr(player) > 12 * 12) {
                // From another dimension she comes back as a new entity; null when she cannot stand near him.
                VikaEntity moved = VikaManager.teleport(existing, serverPlayer.serverLevel(), player.blockPosition());
                if (moved == null) {
                    VikaSpeech.tell(existing, serverPlayer, "stuck", LineFormatter.values());
                    return InteractionResultHolder.fail(stack);
                }
                existing = moved;
            }
            if (existing != null) {
                existing.brain().changeAffection(50);
                existing.brain().needs().add(Needs.Need.ATTENTION, 40);
                existing.brain().makeHappy(600);
                existing.hearts(12);
                existing.playEmote(Emote.CELEBRATE);
                VikaSpeech.say(existing, serverPlayer, "heart.gift", LineFormatter.values());
            }
        } else {
            VikaManager.call(serverPlayer);
            if (VikaManager.find(serverPlayer) == null) {
                return InteractionResultHolder.fail(stack);
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.2F);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.viktoria.laura_heart.tooltip").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("item.viktoria.laura_heart.recipe").withStyle(ChatFormatting.GRAY));
    }
}
