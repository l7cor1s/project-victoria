package com.l7cor1s.viktoria.entity.ai;

import com.l7cor1s.viktoria.config.VikaConfig;
import com.l7cor1s.viktoria.dialogue.LineFormatter;
import com.l7cor1s.viktoria.entity.VikaEntity;
import com.l7cor1s.viktoria.entity.VikaMode;
import com.l7cor1s.viktoria.entity.VikaSpeech;
import com.l7cor1s.viktoria.entity.brain.Needs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.PathType;

import java.util.EnumSet;

/**
 * Follows her partner at a "partner distance", teleports only when left far behind (teleportDistance,
 * 128 blocks by default), and complains when she is stuck. When she craves attention she walks much
 * closer. Yes, on purpose.
 *
 * @author vyrriox
 */
public class VikaFollowOwnerGoal extends Goal {
    private final VikaEntity laura;
    private LivingEntity owner;
    private int recalc;
    private float oldWaterCost;
    private int stuckTicks;

    public VikaFollowOwnerGoal(VikaEntity laura) {
        this.laura = laura;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean blocked() {
        return laura.getMode() != VikaMode.FOLLOW || laura.isOrderedToSit() || laura.brain().isSulking() || laura.isAsleep()
                || laura.isPassenger() || laura.isLeashed() || laura.isFetching();
    }

    private double startDistance() {
        double start = VikaConfig.followRadius.getDouble();
        return clingy() ? Math.min(start, 2.5) : start;
    }

    private double stopDistance() {
        double stop = VikaConfig.followStopDistance.getDouble();
        return clingy() ? 1.4 : stop;
    }

    private boolean clingy() {
        return VikaConfig.needsEnabled.get() && laura.brain().needs().get(Needs.Need.ATTENTION) < 25;
    }

    @Override
    public boolean canUse() {
        LivingEntity o = laura.getOwner();
        if (o == null || o.isSpectator() || blocked() || o.level() != laura.level()) {
            return false;
        }
        if (laura.distanceToSqr(o) < startDistance() * startDistance()) {
            return false;
        }
        this.owner = o;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (owner == null || blocked() || owner.level() != laura.level() || !owner.isAlive()) {
            return false;
        }
        return laura.distanceToSqr(owner) > stopDistance() * stopDistance();
    }

    @Override
    public void start() {
        recalc = 0;
        stuckTicks = 0;
        oldWaterCost = laura.getPathfindingMalus(PathType.WATER);
        laura.setPathfindingMalus(PathType.WATER, 0.0F);
    }

    @Override
    public void stop() {
        owner = null;
        laura.getNavigation().stop();
        laura.setPathfindingMalus(PathType.WATER, oldWaterCost);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        double distSq = laura.distanceToSqr(owner);
        double teleport = VikaConfig.teleportDistance.getDouble();
        if (distSq < teleport * teleport) {
            laura.getLookControl().setLookAt(owner, 10.0F, laura.getMaxHeadXRot());
        }
        if (--recalc <= 0) {
            recalc = adjustedTickDelay(10);
            if (distSq >= teleport * teleport) {
                VikaMovement.teleportNear(laura, owner.blockPosition());
            } else {
                double speed = distSq > 12 * 12 ? 1.25 : 1.0;
                if (VikaConfig.needsEnabled.get() && laura.brain().needs().get(Needs.Need.ENERGY) < 20) {
                    speed *= 0.7;
                }
                laura.getNavigation().moveTo(owner, speed);
            }
        }
        boolean moving = laura.getDeltaMovement().horizontalDistanceSqr() > 0.0004;
        if (!moving && distSq > 10 * 10) {
            if (++stuckTicks >= VikaConfig.stuckSeconds.getInt() * 20) {
                stuckTicks = 0;
                if (VikaConfig.complainWhenStuck.get()) {
                    VikaSpeech.sayToOwner(laura, "stuck", LineFormatter.values());
                }
                if (VikaConfig.teleportWhenStuck.get()) {
                    VikaMovement.teleportNear(laura, owner.blockPosition());
                }
            }
        } else {
            stuckTicks = 0;
        }
    }
}
