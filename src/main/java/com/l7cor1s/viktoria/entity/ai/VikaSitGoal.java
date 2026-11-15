package com.l7cor1s.viktoria.entity.ai;

import com.l7cor1s.viktoria.entity.VikaEntity;
import com.l7cor1s.viktoria.entity.VikaMode;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Sits when told to stay, and while sulking (she refuses to move until you apologize).
 *
 * @author vyrriox
 */
public class VikaSitGoal extends Goal {
    private final VikaEntity laura;

    public VikaSitGoal(VikaEntity laura) {
        this.laura = laura;
        this.setFlags(EnumSet.of(Flag.JUMP, Flag.MOVE));
    }

    private boolean shouldSit() {
        if (laura.isInWaterOrBubble() || !laura.onGround() || laura.isAsleep() || laura.isFetching()) {
            return false;
        }
        return laura.getMode() == VikaMode.STAY || laura.brain().isSulking();
    }

    @Override
    public boolean canUse() {
        return shouldSit();
    }

    @Override
    public boolean canContinueToUse() {
        return shouldSit();
    }

    @Override
    public void start() {
        laura.getNavigation().stop();
        laura.setInSittingPose(true);
    }

    @Override
    public void stop() {
        laura.setInSittingPose(false);
    }
}
