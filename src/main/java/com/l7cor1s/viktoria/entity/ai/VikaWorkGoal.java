package com.l7cor1s.viktoria.entity.ai;

import com.l7cor1s.viktoria.config.VikaConfig;
import com.l7cor1s.viktoria.dialogue.LineFormatter;
import com.l7cor1s.viktoria.entity.Emote;
import com.l7cor1s.viktoria.entity.VikaEntity;
import com.l7cor1s.viktoria.entity.VikaJob;
import com.l7cor1s.viktoria.entity.VikaMode;
import com.l7cor1s.viktoria.entity.VikaSpeech;
import com.l7cor1s.viktoria.entity.brain.Needs;
import com.l7cor1s.viktoria.entity.work.CookWork;
import com.l7cor1s.viktoria.entity.work.FarmerWork;
import com.l7cor1s.viktoria.entity.work.VikaTask;
import com.l7cor1s.viktoria.entity.work.LumberjackWork;
import com.l7cor1s.viktoria.entity.work.Work;
import com.l7cor1s.viktoria.entity.work.WorkArea;
import com.l7cor1s.viktoria.entity.work.WorkContext;
import com.l7cor1s.viktoria.world.VikaAdvancements;
import com.l7cor1s.viktoria.world.VikaWorldChecks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Runs her errands (chop a tree, harvest, cook) and, when the queue is empty and she is in WORK
 * mode, rotates between her active jobs.
 * <p>
 * Work that finds nothing to do is reported once, then tried again after a wait that grows (one
 * minute, two, four...), without a word for each new attempt.
 *
 * @author vyrriox
 */
public class VikaWorkGoal extends Goal {
    /** First wait after work that found nothing to do; doubled each time it happens again. */
    public static final int RETRY_TICKS = 20 * 60;
    /** Longest wait of an errand between two attempts. */
    private static final int MAX_ERRAND_RETRY_TICKS = RETRY_TICKS * 8;
    /** Longest pause of her jobs between two rounds. */
    private static final int MAX_JOB_PAUSE_TICKS = RETRY_TICKS * 4;
    /** A failure older than this no longer makes the next wait longer. */
    private static final int FORGET_TICKS = 20 * 60 * 20;
    /** A job that kept her busy at least this long did real work, not just a look around. */
    private static final int BUSY_TICKS = 40;

    private final VikaEntity laura;
    private final WorkContext ctx;
    private Work work;
    private VikaJob workJob = VikaJob.NONE;
    private VikaTask workTask;
    private List<ItemStack> deliveries;
    private int rotateIndex;
    private int idleJobs;
    private int idleRounds;
    private int busyTicks;
    private long pauseUntil;
    private int deliverTicks;
    private boolean griefingSaid;
    private final Set<VikaJob> failSaid = EnumSet.noneOf(VikaJob.class);
    private final Map<VikaTask.Type, Integer> errandFails = new EnumMap<>(VikaTask.Type.class);
    private final Map<VikaTask.Type, Long> errandRetryAt = new EnumMap<>(VikaTask.Type.class);
    /** Queued errands that were waiting behind one that found nothing: they run without a word. */
    private final Set<VikaTask> quietErrands = Collections.newSetFromMap(new IdentityHashMap<>());

    public VikaWorkGoal(VikaEntity laura) {
        this.laura = laura;
        this.ctx = new WorkContext(laura);
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean blocked() {
        return !VikaConfig.workEnabled.get() || laura.isFetching() || laura.isAsleep() || laura.isOrderedToSit()
                || laura.brain().isSulking() || laura.isPassenger() || laura.isLeashed() || laura.getTarget() != null;
    }

    private VikaTask errand() {
        VikaTask task = laura.workplace().current();
        return task != null && task.type().isErrand() && task.type() != VikaTask.Type.FETCH ? task : null;
    }

    private boolean jobsActive() {
        if (laura.getMode() != VikaMode.WORK || !laura.workplace().hasJobs()) {
            return false;
        }
        return VikaConfig.workAtNight.get() || !VikaWorldChecks.isNight(laura.level());
    }

    private long now() {
        return laura.level().getGameTime();
    }

    private boolean paused() {
        return now() < pauseUntil;
    }

    /** Jobs and errands that break blocks stop while the mobGriefing game rule is off. */
    private boolean mayBreakBlocks() {
        return laura.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
    }

    @Override
    public boolean canUse() {
        if (blocked()) {
            return false;
        }
        if (errand() != null || deliveries != null) {
            return true;
        }
        return !paused() && jobsActive();
    }

    @Override
    public boolean canContinueToUse() {
        if (blocked()) {
            return false;
        }
        return errand() != null || deliveries != null || jobsActive() && !paused();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void stop() {
        if (work != null) {
            work.stop();
        }
        laura.getNavigation().stop();
    }

    /** Drops the running work (the task changed or the jobs were edited). */
    public void reset() {
        if (work != null) {
            work.stop();
        }
        work = null;
        workTask = null;
        workJob = VikaJob.NONE;
        deliveries = null;
        pauseUntil = 0;
        idleJobs = 0;
        idleRounds = 0;
        busyTicks = 0;
        griefingSaid = false;
        failSaid.clear();
    }

    /** True while a job or an errand is under way (read by the self tests). */
    public boolean isWorking() {
        return work != null;
    }

    // ------------------------------------------------------------------ errands that found nothing

    /** True while a queued errand should wait: the last one of its kind found nothing to do. */
    public boolean isWaiting(VikaTask task) {
        Long retryAt = errandRetryAt.get(task.type());
        return retryAt != null && now() < retryAt;
    }

    /** True for an errand that starts and gives up without a word (the first failure already said why). */
    public boolean isQuiet(VikaTask task) {
        return quietErrands.contains(task);
    }

    /** A new order of her partner: she tries right away, whatever happened before. */
    public void forgetFailures(VikaTask.Type type) {
        errandFails.remove(type);
        errandRetryAt.remove(type);
    }

    /** Ticks she waits before the next attempt, after {@code fails} attempts in a row found nothing. */
    public static int retryDelay(int fails, int max) {
        return (int) Math.min(max, (long) RETRY_TICKS << Math.min(16, Math.max(0, fails - 1)));
    }

    private void failErrand(VikaTask task, String key) {
        if (work != null) {
            work.stop();
        }
        work = null;
        workTask = null;
        boolean quiet = quietErrands.contains(task);
        List<VikaTask> queued = laura.workplace().queued();
        quietErrands.retainAll(queued);
        if (!quiet) {
            VikaSpeech.sayToOwner(laura, key, LineFormatter.values());
            laura.playEmote(Emote.SHRUG);
        }
        VikaTask.Type type = task.type();
        long now = now();
        Long last = errandRetryAt.get(type);
        int fails = last != null && now - last < FORGET_TICKS ? errandFails.getOrDefault(type, 0) + 1 : 1;
        errandFails.put(type, fails);
        errandRetryAt.put(type, now + retryDelay(fails, MAX_ERRAND_RETRY_TICKS));
        // The same errands waiting behind it would find the same nothing: they wait, then try quietly.
        for (VikaTask other : queued) {
            if (other.type() == type) {
                quietErrands.add(other);
            }
        }
        laura.workplace().finishCurrent();
    }

    @Override
    public void tick() {
        if (deliveries != null) {
            deliver();
            return;
        }
        VikaTask task = errand();
        if (task != null) {
            runErrand(task);
            return;
        }
        if (workTask != null) {
            // The errand was cancelled from outside.
            reset();
        }
        runJobs();
    }

    private WorkArea areaFor(VikaTask task, int defaultRadius) {
        if (task.area() != null) {
            return task.area();
        }
        return new WorkArea(laura.blockPosition(), defaultRadius, laura.level().dimension());
    }

    private void runErrand(VikaTask task) {
        if (task.type() != VikaTask.Type.COOK && !mayBreakBlocks()) {
            // Ordered or queued before the rule was turned off, or turned off while she was at it:
            // she puts her tools down at once.
            failErrand(task, "work.no_griefing");
            return;
        }
        if (work == null || workTask != task) {
            if (work != null) {
                work.stop();
            }
            workTask = task;
            workJob = VikaJob.NONE;
            boolean self = "self".equals(task.arg());
            work = switch (task.type()) {
                case CHOP_TREE -> new LumberjackWork(ctx, areaFor(task, 16), true);
                case HARVEST -> new FarmerWork(ctx, areaFor(task, 12), true);
                case COOK -> new CookWork(ctx, areaFor(task, 10), true, self);
                default -> null;
            };
            if (work == null) {
                laura.workplace().finishCurrent();
                return;
            }
        }
        Work.Status status = work.tick();
        switch (status) {
            case DONE -> {
                List<ItemStack> products = new ArrayList<>(work.products());
                work.stop();
                work = null;
                workTask = null;
                forgetFailures(task.type());
                quietErrands.remove(task);
                if (products.isEmpty()) {
                    laura.workplace().finishCurrent();
                } else {
                    deliveries = products;
                    deliverTicks = 0;
                    ctx.resetWalk();
                }
                ServerPlayer owner = VikaSpeech.owner(laura);
                if (owner != null) {
                    VikaAdvancements.award(owner, "task_" + task.type().key());
                }
            }
            case FAILED, IDLE -> failErrand(task, work.failKey());
            case WORKING -> {
            }
        }
    }

    private void deliver() {
        ServerPlayer owner = VikaSpeech.owner(laura);
        deliverTicks++;
        if (owner == null || owner.level() != laura.level() || deliverTicks > 20 * 60) {
            // Nobody to give it to: she keeps everything.
            deliveries = null;
            laura.workplace().finishCurrent();
            return;
        }
        double teleport = VikaConfig.teleportDistance.getDouble();
        if (laura.distanceToSqr(owner) > teleport * teleport) {
            VikaMovement.teleportNear(laura, owner.blockPosition());
        }
        if (!ctx.walkTo(owner.blockPosition(), 2.5)) {
            return;
        }
        int total = 0;
        ItemStack first = ItemStack.EMPTY;
        for (ItemStack wanted : deliveries) {
            List<ItemStack> taken = ctx.take(s -> ItemStack.isSameItemSameComponents(s, wanted), wanted.getCount());
            for (ItemStack s : taken) {
                if (first.isEmpty()) {
                    first = s.copy();
                }
                total += s.getCount();
                laura.brain().giveToPlayer(owner, s);
            }
        }
        deliveries = null;
        laura.workplace().finishCurrent();
        laura.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        laura.playEmote(Emote.BOW);
        VikaSpeech.say(laura, owner, "work.delivered", LineFormatter.values().with("count", total).with("item", first.isEmpty() ? "" : first.getHoverName()));
    }

    /**
     * The current job has nothing for her: next job. Once every job said so, she pauses, longer each
     * time in a row, and says it once ({@code announce}) until one of her jobs kept her busy again.
     */
    private void jobIdle(int jobCount, boolean announce) {
        if (busyTicks > BUSY_TICKS) {
            idleJobs = 0;
            idleRounds = 0;
        }
        busyTicks = 0;
        rotateIndex++;
        idleJobs++;
        if (idleJobs < jobCount) {
            return;
        }
        idleJobs = 0;
        idleRounds++;
        pauseUntil = now() + retryDelay(idleRounds, MAX_JOB_PAUSE_TICKS);
        if (announce && idleRounds == 1 && laura.brain().readyPublic("work_idle", 900)) {
            VikaSpeech.sayToOwner(laura, "work.idle", LineFormatter.values());
        }
    }

    private void runJobs() {
        Map<VikaJob, WorkArea> jobs = laura.workplace().jobs();
        if (jobs.isEmpty()) {
            return;
        }
        List<VikaJob> list = new ArrayList<>(jobs.keySet());
        if (work != null && workJob != VikaJob.COOK && !mayBreakBlocks()) {
            // The rule was turned off while she was at it: the round stops here.
            work.stop();
            work = null;
        }
        if (work == null || !jobs.containsKey(workJob)) {
            if (work != null) {
                work.stop();
            }
            busyTicks = 0;
            workJob = list.get(Math.floorMod(rotateIndex, list.size()));
            WorkArea area = jobs.get(workJob);
            if (workJob != VikaJob.COOK && !mayBreakBlocks()) {
                // The rule was turned off after the job was given: she leaves the blocks alone.
                work = null;
                if (!griefingSaid && laura.brain().readyPublic("work_no_griefing", 900)) {
                    VikaSpeech.sayToOwner(laura, "work.no_griefing", LineFormatter.values());
                }
                griefingSaid = true;
                jobIdle(list.size(), false);
                return;
            }
            griefingSaid = false;
            work = switch (workJob) {
                case LUMBERJACK -> new LumberjackWork(ctx, area, false);
                case FARMER -> new FarmerWork(ctx, area, false);
                case COOK -> new CookWork(ctx, area, false, false);
                default -> null;
            };
            if (work == null) {
                rotateIndex++;
                return;
            }
        }
        WorkArea area = jobs.get(workJob);
        if (area != null && !area.isIn(laura.level())) {
            return;
        }
        if (area != null && !area.contains(laura.blockPosition()) && laura.distanceToSqr(area.center().getX(), area.center().getY(), area.center().getZ()) > 48 * 48) {
            VikaMovement.teleportNear(laura, area.center());
        }
        Work.Status status = work.tick();
        switch (status) {
            case WORKING -> {
                busyTicks++;
                if (busyTicks > BUSY_TICKS) {
                    failSaid.remove(workJob);
                }
            }
            case IDLE, DONE -> {
                work.stop();
                work = null;
                jobIdle(list.size(), true);
            }
            case FAILED -> {
                // Said once, until this job kept her busy again.
                if (failSaid.add(workJob) && laura.brain().readyPublic("work_fail_" + workJob.key(), 300)) {
                    VikaSpeech.sayToOwner(laura, work.failKey(), LineFormatter.values());
                }
                work.stop();
                work = null;
                jobIdle(list.size(), false);
            }
        }
        if (laura.tickCount % 1200 == 0 && VikaConfig.needsEnabled.get() && laura.brain().needs().get(Needs.Need.FUN) < 25
                && laura.brain().readyPublic("work_bored", 600)) {
            VikaSpeech.sayToOwner(laura, "work.bored", LineFormatter.values());
            laura.playEmote(Emote.TAP_FOOT);
        }
    }
}
