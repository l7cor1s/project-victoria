package com.l7cor1s.viktoria.entity.ai;

import com.l7cor1s.viktoria.config.VikaConfig;
import com.l7cor1s.viktoria.dialogue.LineFormatter;
import com.l7cor1s.viktoria.entity.VikaEntity;
import com.l7cor1s.viktoria.entity.VikaSpeech;
import com.l7cor1s.viktoria.entity.brain.Needs;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.Optional;

/**
 * When she feels dirty she walks into the nearest water for a quick bath. What the water does to
 * her is in {@link com.l7cor1s.viktoria.entity.brain.VikaBrain}, whoever brought her to it.
 *
 * @author vyrriox
 */
public class VikaBathGoal extends Goal {
    private final VikaEntity laura;
    private BlockPos water;
    private int ticks;
    private int bathTicks;
    private int cooldown;

    public VikaBathGoal(VikaEntity laura) {
        this.laura = laura;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (--cooldown > 0) {
            return false;
        }
        cooldown = 300;
        if (!VikaConfig.needsEnabled.get() || laura.isFetching() || laura.isAsleep() || laura.isOrderedToSit()
                || laura.brain().needs().get(Needs.Need.HYGIENE) > 25) {
            return false;
        }
        Optional<BlockPos> found = BlockPos.findClosestMatch(laura.blockPosition(), 14, 4,
                pos -> laura.level().getFluidState(pos).is(FluidTags.WATER) && laura.level().getBlockState(pos.above()).isAir());
        water = found.map(BlockPos::immutable).orElse(null);
        return water != null;
    }

    @Override
    public boolean canContinueToUse() {
        return water != null && ticks < 400 && bathTicks < 100;
    }

    @Override
    public void start() {
        ticks = 0;
        bathTicks = 0;
        VikaSpeech.sayToOwner(laura, "need.hygiene.bath", LineFormatter.values());
    }

    @Override
    public void tick() {
        ticks++;
        if (laura.isInWater()) {
            // Her brain tells when the water has washed her (line and advancement), as for any bath.
            bathTicks++;
            laura.getNavigation().stop();
            return;
        }
        if (ticks % 20 == 1) {
            laura.getNavigation().moveTo(water.getX() + 0.5, water.getY(), water.getZ() + 0.5, 1.1);
        }
    }

    @Override
    public void stop() {
        water = null;
        laura.getNavigation().stop();
    }
}
