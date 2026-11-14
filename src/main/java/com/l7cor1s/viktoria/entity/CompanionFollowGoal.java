package com.l7cor1s.viktoria.entity;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class CompanionFollowGoal extends Goal {
    private final CompanionEntity companion;
    private final double speed;
    private Player owner;
    private int recalcCooldown;

    public CompanionFollowGoal(CompanionEntity companion, double speed) {
        this.companion = companion;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!companion.isFollowing()) {
            return false;
        }
        Player nearest = companion.level().getNearestPlayer(companion, 48.0);
        if (nearest == null || nearest.isSpectator()) {
            return false;
        }
        this.owner = nearest;
        return companion.distanceToSqr(owner) > 9.0;
    }

    @Override
    public boolean canContinueToUse() {
        return companion.isFollowing()
                && owner != null
                && owner.isAlive()
                && !owner.isSpectator()
                && companion.distanceToSqr(owner) > 4.0;
    }

    @Override
    public void start() {
        recalcCooldown = 0;
        companion.setAnimState("follow");
    }

    @Override
    public void stop() {
        owner = null;
        companion.getNavigation().stop();
        if (!"talk".equals(companion.getAnimState()) && !"gift".equals(companion.getAnimState())) {
            companion.setAnimState("idle");
        }
    }

    @Override
    public void tick() {
        if (owner == null) {
            return;
        }
        companion.getLookControl().setLookAt(owner, 10.0f, companion.getMaxHeadXRot());
        if (--recalcCooldown <= 0) {
            recalcCooldown = 10;
            companion.getNavigation().moveTo(owner, speed);
        }
        if (companion.distanceToSqr(owner) > 144.0) {
            companion.teleportTo(owner.getX(), owner.getY(), owner.getZ());
        }
    }
}
