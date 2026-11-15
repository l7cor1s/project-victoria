package com.l7cor1s.viktoria.entity.ai;

import com.l7cor1s.viktoria.config.VikaConfig;
import com.l7cor1s.viktoria.dialogue.LineFormatter;
import com.l7cor1s.viktoria.entity.VikaEntity;
import com.l7cor1s.viktoria.entity.VikaSpeech;
import com.l7cor1s.viktoria.entity.brain.Needs;
import com.l7cor1s.viktoria.entity.work.ChestPurpose;
import com.l7cor1s.viktoria.gift.GiftTable;
import com.l7cor1s.viktoria.platform.InventoryAccess;
import com.l7cor1s.viktoria.platform.VikaInventories;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

/**
 * When she is hungry with nothing to eat, she goes to her pantry chest (or raids the nearest storage
 * when she is starving).
 *
 * @author vyrriox
 */
public class VikaFindFoodGoal extends Goal {
    private static final int RADIUS = 16;
    private static final Predicate<ItemStack> EDIBLE = stack -> {
        GiftTable.FoodInfo food = GiftTable.food(stack);
        return food != null && food.preference() != GiftTable.Preference.DISLIKED;
    };

    private final VikaEntity laura;
    private BlockPos target;
    private int ticks;
    private int cooldown;
    private boolean pantry;

    public VikaFindFoodGoal(VikaEntity laura) {
        this.laura = laura;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (--cooldown > 0) {
            return false;
        }
        cooldown = 200;
        if (!VikaConfig.needsEnabled.get() || laura.isFetching() || laura.isAsleep() || laura.isOrderedToSit() || laura.isGagged()
                || laura.brain().hasFood() || !(laura.level() instanceof ServerLevel level)) {
            return false;
        }
        float hunger = laura.brain().needs().get(Needs.Need.HUNGER);
        List<BlockPos> pantries = laura.workplace().assigned(ChestPurpose.PANTRY);
        if (hunger < 35) {
            for (BlockPos pos : pantries) {
                InventoryAccess inv = VikaInventories.at(level, pos);
                if (inv != null && inv.hasAnyMatching(EDIBLE) && pos.distSqr(laura.blockPosition()) < 48 * 48) {
                    target = pos;
                    pantry = true;
                    return true;
                }
            }
        }
        if (hunger > 12 || !VikaConfig.searchFoodInContainers.get()) {
            return false;
        }
        for (BlockPos pos : VikaInventories.storagesAround(level, laura.blockPosition(), RADIUS, p -> true)) {
            InventoryAccess inv = VikaInventories.at(level, pos);
            // Even starving, she only opens a container her partner could open too.
            if (inv != null && inv.hasAnyMatching(EDIBLE) && VikaInventories.mayUse(laura, pos)) {
                target = pos;
                pantry = false;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && ticks < 400;
    }

    @Override
    public void start() {
        ticks = 0;
        VikaSpeech.sayToOwner(laura, pantry ? "need.hunger.pantry" : "need.hunger.searching", LineFormatter.values());
    }

    @Override
    public void tick() {
        ticks++;
        if (laura.distanceToSqr(target.getX() + 0.5, target.getY(), target.getZ() + 0.5) > 2.3 * 2.3) {
            if (ticks % 20 == 1) {
                laura.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.1);
            }
            if (ticks > 300) {
                VikaMovement.teleportNear(laura, target);
            }
            return;
        }
        InventoryAccess inv = laura.level() instanceof ServerLevel level ? VikaInventories.at(level, target) : null;
        if (inv != null && !VikaInventories.isLocked(laura, target)) {
            for (int slot = 0; slot < inv.size(); slot++) {
                ItemStack stack = inv.get(slot);
                if (!stack.isEmpty() && EDIBLE.test(stack)) {
                    ItemStack taken = inv.extract(slot, Math.min(pantry ? 4 : 3, stack.getCount()));
                    ItemStack rest = laura.bags().add(taken);
                    if (!rest.isEmpty()) {
                        laura.spawnAtLocation(rest);
                    }
                    laura.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    VikaSpeech.sayToOwner(laura, "need.hunger.found_food", LineFormatter.values().with("item", taken.getHoverName()));
                    break;
                }
            }
        }
        target = null;
    }

    @Override
    public void stop() {
        target = null;
        laura.getNavigation().stop();
    }
}
