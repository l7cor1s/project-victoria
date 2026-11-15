package com.l7cor1s.viktoria.world;

import com.l7cor1s.viktoria.ViktoriaMod;
import com.l7cor1s.viktoria.config.VikaConfig;
import com.l7cor1s.viktoria.cooking.CookingSupport;
import com.l7cor1s.viktoria.dialogue.DialogueManager;
import com.l7cor1s.viktoria.dialogue.LineFormatter;
import com.l7cor1s.viktoria.dialogue.TextMatcher;
import com.l7cor1s.viktoria.entity.CombatMode;
import com.l7cor1s.viktoria.entity.Emote;
import com.l7cor1s.viktoria.entity.VikaBags;
import com.l7cor1s.viktoria.entity.VikaEntity;
import com.l7cor1s.viktoria.entity.VikaJob;
import com.l7cor1s.viktoria.entity.VikaMode;
import com.l7cor1s.viktoria.entity.VikaSpeech;
import com.l7cor1s.viktoria.entity.ai.FetchGoal;
import com.l7cor1s.viktoria.entity.ai.VikaMovement;
import com.l7cor1s.viktoria.entity.brain.Needs;
import com.l7cor1s.viktoria.entity.work.ChestPurpose;
import com.l7cor1s.viktoria.entity.work.VikaTask;
import com.l7cor1s.viktoria.entity.work.VikaWorkplace;
import com.l7cor1s.viktoria.entity.work.WorkArea;
import com.l7cor1s.viktoria.gift.GiftTable;
import com.l7cor1s.viktoria.network.VikaAction;
import com.l7cor1s.viktoria.network.VikaNetwork;
import com.l7cor1s.viktoria.registry.VikaRegistries;
import com.l7cor1s.viktoria.util.ItemSpec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Every order Laura can receive, whatever its source (menu, chat, command). Permission checks,
 * refusals (she can be moody), and the dialogue answers all happen here.
 *
 * @author vyrriox
 */
public final class VikaActions {
    public enum Source {
        MENU, CHAT, COMMAND
    }

    private VikaActions() {
    }

    public static boolean canCommand(ServerPlayer player, VikaEntity laura) {
        return laura.isOwnedBy(player) || VikaConfig.othersCanInteract.get() || player.hasPermissions(2);
    }

    /**
     * Orders she might refuse when she is in a bad mood. A hug and a kiss are not orders: a hug is
     * how her partner comforts her, and both have their own rules in {@link #hug} and {@link #kiss}.
     */
    private static boolean isRefusable(VikaAction action, String arg) {
        return switch (action) {
            case FOLLOW, STAY, WANDER, HOME, COME, FETCH, SLEEP, EAT -> true;
            case EMOTE -> !isCuddle(Emote.byName(arg));
            default -> false;
        };
    }

    /** The hug and the kiss of the emote wheel are the real ones, not an animation played on order: see {@link #emote}. */
    private static boolean isCuddle(Emote emote) {
        return emote == Emote.HUG || emote == Emote.KISS;
    }

    /** Returns false when nothing was done: the player may not command her, or she refused. */
    public static boolean perform(ServerPlayer player, VikaEntity laura, VikaAction action, String arg, Source source) {
        if (!canCommand(player, laura)) {
            VikaSpeech.refuse(laura, player);
            return false;
        }
        String a = arg == null ? "" : arg.trim();
        if (source == Source.MENU && (action == VikaAction.TASK || action == VikaAction.FETCH) && laura.workplace().isRepeatedRequest(action.name() + "|" + a)) {
            // The same button twice within half a second is one click: one task, one answer.
            return false;
        }
        // She only argues face to face. An order sent from out of earshot (the command, the call key)
        // is obeyed, like the recall of a companion who is not loaded: her refusal would not be heard.
        if (isRefusable(action, a) && VikaSpeech.inEarshot(laura, player) && laura.brain().refuses(player, action.name())) {
            return false;
        }
        // Going home or coming over can take her to another dimension, where she is a new entity.
        VikaEntity live = laura;
        switch (action) {
            case FOLLOW -> {
                clearMovementTasks(laura);
                laura.wakeUp();
                laura.setMode(VikaMode.FOLLOW);
                VikaSpeech.say(laura, player, "order.follow", LineFormatter.values());
            }
            case STAY -> {
                laura.setMode(VikaMode.STAY);
                VikaSpeech.say(laura, player, "order.stay", LineFormatter.values());
            }
            case WANDER -> {
                laura.wakeUp();
                laura.setMode(VikaMode.WANDER);
                VikaSpeech.say(laura, player, "order.wander", LineFormatter.values());
            }
            case HOME -> live = goHome(player, laura);
            case SET_HOME -> setHome(player, laura);
            case CLEAR_HOME -> {
                laura.setHome(null, null);
                if (laura.getMode() == VikaMode.HOME) {
                    laura.setMode(VikaMode.FOLLOW);
                }
                VikaSpeech.say(laura, player, "home.cleared", LineFormatter.values());
            }
            case COME -> live = come(player, laura);
            case FETCH -> {
                // "item", or "item|count" and "item|count|queue" from the menu. The limit keeps the
                // empty parts, so an argument made of separators only still has a first part.
                String[] parts = a.split("\\|", -1);
                fetch(player, laura, parts[0], parts.length > 1 ? parseInt(parts[1], 0) : 0, parts.length > 2 && parts[2].equalsIgnoreCase("queue"));
            }
            case JOB -> {
                String[] parts = a.split(":", -1);
                VikaJob job = VikaJob.byName(parts[0]);
                boolean on = parts.length > 1 && parts[1].equalsIgnoreCase("on");
                if (on && job != null && job != VikaJob.NONE) {
                    enableJob(player, laura, job, parts.length > 2 ? parseInt(parts[2], 0) : 0);
                } else {
                    disableJob(player, laura, job == null || job == VikaJob.NONE ? null : job);
                }
            }
            case WORK -> backToWork(player, laura);
            case TASK -> {
                String[] parts = a.split(":", -1);
                VikaTask.Type type = VikaTask.Type.byName(parts[0]);
                if (type == null || !type.isErrand() || type == VikaTask.Type.FETCH) {
                    VikaSpeech.say(laura, player, "confused", LineFormatter.values());
                } else {
                    task(player, laura, type, parts.length > 2 ? parseInt(parts[2], 0) : 0, parts.length > 1 && parts[1].equalsIgnoreCase("queue"));
                }
            }
            case QUEUE -> {
                if (a.equalsIgnoreCase("clear")) {
                    laura.workplace().clearQueue();
                    laura.workGoal().reset();
                    player.sendSystemMessage(Component.translatable("viktoria.queue.cleared"));
                } else if (a.startsWith("remove:")) {
                    boolean ok = laura.workplace().removeQueued(parseInt(a.substring(7), 0) - 1);
                    player.sendSystemMessage(Component.translatable(ok ? "viktoria.queue.removed" : "viktoria.queue.invalid"));
                }
            }
            case CHEST -> {
                if (a.equalsIgnoreCase("remove")) {
                    assignChest(player, laura, null);
                } else {
                    ChestPurpose purpose = ChestPurpose.byName(a);
                    if (purpose == null) {
                        VikaSpeech.say(laura, player, "confused", LineFormatter.values());
                    } else {
                        assignChest(player, laura, purpose);
                    }
                }
            }
            case EMOTE -> emote(player, laura, Emote.byName(a));
            case HUG -> hug(player, laura);
            case KISS -> kiss(player, laura);
            case COMPLIMENT -> {
                laura.brain().onCompliment();
                VikaSpeech.respond(laura, player, "beautiful", LineFormatter.values());
            }
            case UNGAG -> ungag(player, laura, false);
            case INVENTORY -> laura.openInventory(player);
            case COMBAT -> {
                CombatMode mode = parseCombat(a, laura.getCombatMode());
                laura.setCombatMode(mode);
                VikaSpeech.say(laura, player, "combat." + mode.key(), LineFormatter.values());
            }
            case PICKUP -> {
                boolean on = a.isEmpty() ? !laura.isPickingUpItems() : a.equalsIgnoreCase("on") || a.equalsIgnoreCase("true");
                laura.setPickingUpItems(on);
                VikaSpeech.say(laura, player, on ? "pickup.on" : "pickup.off", LineFormatter.values());
            }
            case INFO -> sendInfo(player, laura);
            case ANSWER -> {
                if (!laura.brain().answer(player, a.equalsIgnoreCase("yes") || a.equalsIgnoreCase("oui"))) {
                    VikaSpeech.say(laura, player, "confused", LineFormatter.values());
                }
            }
            case EAT -> {
                if (!laura.brain().hasFood()) {
                    VikaSpeech.say(laura, player, "need.hunger.no_food", LineFormatter.values());
                } else {
                    eatBestFood(player, laura);
                }
            }
            case STOP -> {
                laura.fetchGoal().cancel(true);
                laura.workplace().clearQueue();
                laura.workGoal().reset();
                laura.playEmote(Emote.NONE);
                laura.getNavigation().stop();
                VikaSpeech.say(laura, player, "order.stop", LineFormatter.values());
            }
            case RENAME -> rename(player, laura, a);
            case SLEEP -> {
                if (VikaConfig.needsEnabled.get() && laura.brain().needs().get(Needs.Need.ENERGY) > 85 && !VikaWorldChecks.isNight(laura.level())) {
                    VikaSpeech.say(laura, player, "sleep.not_tired", LineFormatter.values());
                } else {
                    laura.goToSleep(laura.findFreeBed(laura.blockPosition(), 8));
                    // An order: the sleep goal ends it when she is rested, whatever her mode.
                    laura.markSleepOrdered();
                    VikaSpeech.say(laura, player, "sleep.good_night", LineFormatter.values());
                }
            }
            case WAKE_UP -> {
                if (laura.isAsleep()) {
                    laura.wakeUp();
                    VikaSpeech.say(laura, player, "sleep.woken_up", LineFormatter.values());
                    laura.brain().changeAffection(-2);
                }
            }
        }
        VikaNetwork.sendStatus(player, live);
        return true;
    }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static void clearMovementTasks(VikaEntity laura) {
        laura.fetchGoal().cancel(true);
    }

    private static CombatMode parseCombat(String a, CombatMode current) {
        for (CombatMode mode : CombatMode.values()) {
            if (mode.name().equalsIgnoreCase(a)) {
                return mode;
            }
        }
        return CombatMode.byId((current.ordinal() + 1) % CombatMode.values().length);
    }

    // ------------------------------------------------------------------ movement orders

    public static void setHome(ServerPlayer player, VikaEntity laura) {
        laura.setHome(player.blockPosition(), player.level().dimension());
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 0.5, player.getZ(), 12, 0.6, 0.4, 0.6, 0.0);
        }
        VikaSpeech.say(laura, player, "home.set", LineFormatter.values());
    }

    /** Returns her as she is afterwards: a new entity when her home is in another dimension. */
    public static VikaEntity goHome(ServerPlayer player, VikaEntity laura) {
        BlockPos home = laura.getHomePos();
        if (home == null) {
            VikaSpeech.say(laura, player, "home.none", LineFormatter.values());
            return laura;
        }
        laura.wakeUp();
        laura.fetchGoal().cancel(true);
        laura.setMode(VikaMode.HOME);
        VikaSpeech.tell(laura, player, "home.go", LineFormatter.values());
        ServerLevel homeLevel = player.getServer().getLevel(laura.getHomeDimension());
        if (homeLevel == null) {
            return laura;
        }
        boolean otherDimension = homeLevel != laura.level();
        double far = VikaConfig.homeTeleportDistance.getInt();
        if (otherDimension || laura.blockPosition().distSqr(home) > far * far) {
            VikaEntity moved = VikaManager.teleport(laura, homeLevel, home);
            if (moved == null) {
                // Her home is walled in or gone: she says so and stays where she is.
                VikaSpeech.tell(laura, player, "stuck", LineFormatter.values());
                return laura;
            }
            return moved;
        }
        return laura;
    }

    /** Returns her as she is afterwards: a new entity when she came from another dimension. */
    public static VikaEntity come(ServerPlayer player, VikaEntity laura) {
        laura.wakeUp();
        laura.setMode(VikaMode.FOLLOW);
        double far = VikaConfig.teleportDistance.getDouble();
        boolean jump = laura.level() != player.level() || laura.distanceToSqr(player) > far * far;
        if (!jump) {
            // A call is an order: when no path leads to the player at all, she comes anyway.
            // Beyond her path range a partial path is normal: she walks it and searches again.
            double range = laura.getAttributeValue(Attributes.FOLLOW_RANGE);
            Path path = laura.getNavigation().createPath(player, 1);
            boolean walkable = path != null && (path.canReach() || laura.distanceToSqr(player) > range * range);
            if (walkable) {
                laura.getNavigation().moveTo(path, 1.2);
            } else {
                jump = true;
            }
        }
        if (jump) {
            VikaEntity moved = VikaManager.teleport(laura, player.serverLevel(), player.blockPosition());
            if (moved == null) {
                // Nowhere safe to stand next to him (he flies, or hangs over the void): she says so and stays.
                VikaSpeech.tell(laura, player, "stuck", LineFormatter.values());
                return laura;
            }
            laura = moved;
        }
        VikaSpeech.say(laura, player, "order.come", LineFormatter.values());
        return laura;
    }

    // ------------------------------------------------------------------ fetch

    /**
     * Starts (or queues) a fetch. {@code what} is an item id, a #tag, "held" (the item in the player's
     * hand), or free text matched against the item keywords of the dialogue files.
     */
    public static void fetch(ServerPlayer player, VikaEntity laura, String what, int count, boolean queue) {
        ItemSpec spec = resolveItem(player, what);
        if (spec == null) {
            VikaSpeech.say(laura, player, "fetch.what", LineFormatter.values());
            return;
        }
        int amount = count > 0 ? count : spec.item() != null ? Math.min(spec.icon().getMaxStackSize(), 16) : 16;
        if (queue || laura.workplace().current() != null) {
            if (laura.workplace().enqueue(new VikaTask(VikaTask.Type.FETCH, spec.raw(), amount, null))) {
                VikaSpeech.say(laura, player, "task.queued", LineFormatter.values().with("task", Component.translatable("viktoria.task.fetch")));
            } else {
                VikaSpeech.say(laura, player, "task.queue_full", LineFormatter.values());
            }
            return;
        }
        startFetch(player, laura, spec, amount);
    }

    private static void startFetch(ServerPlayer player, VikaEntity laura, ItemSpec spec, int amount) {
        FetchGoal.Result result = laura.fetchGoal().request(player, spec, amount);
        switch (result) {
            case STARTED -> VikaSpeech.say(laura, player, "fetch.start", LineFormatter.values().with("item", spec.displayName()).with("count", amount));
            case DISABLED -> VikaSpeech.say(laura, player, "fetch.disabled", LineFormatter.values());
            case BUSY -> VikaSpeech.say(laura, player, "fetch.busy", LineFormatter.values());
            case INVALID -> VikaSpeech.say(laura, player, "fetch.what", LineFormatter.values());
        }
    }

    @Nullable
    public static ItemSpec resolveItem(ServerPlayer player, String what) {
        String w = what == null ? "" : what.trim();
        if (w.isEmpty() || w.equalsIgnoreCase("held") || w.equalsIgnoreCase("this")) {
            ItemStack held = player.getMainHandItem();
            return held.isEmpty() ? null : ItemSpec.of(held.getItem());
        }
        Optional<ItemSpec> direct = ItemSpec.parse(w.toLowerCase(Locale.ROOT).replace(' ', '_'));
        if (direct.isPresent()) {
            return direct.get();
        }
        List<String> keywords = DialogueManager.matchItems(TextMatcher.Prepared.of(w), player.clientInformation().language());
        for (String keyword : keywords) {
            Optional<ItemSpec> spec = ItemSpec.parse(keyword);
            if (spec.isPresent()) {
                return spec.get();
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ tasks and jobs

    /** Adds an errand. With {@code queue} false it replaces the current errand. */
    public static void task(ServerPlayer player, VikaEntity laura, VikaTask.Type type, int radius, boolean queue) {
        if (type.isErrand() && !VikaConfig.workEnabled.get()) {
            VikaSpeech.say(laura, player, "work.disabled", LineFormatter.values());
            return;
        }
        if (type.isErrand() && !mobGriefingAllowed(laura) && type != VikaTask.Type.COOK) {
            VikaSpeech.say(laura, player, "work.no_griefing", LineFormatter.values());
            return;
        }
        int r = Math.max(3, Math.min(VikaConfig.workMaxRadius.getInt(), radius > 0 ? radius : VikaConfig.workRadius.getInt()));
        WorkArea area = new WorkArea(player.blockPosition(), r, player.level().dimension());
        VikaTask task = new VikaTask(type, "", 0, area);
        VikaWorkplace work = laura.workplace();
        if (queue && (work.current() != null || laura.fetchGoal().isActive())) {
            if (work.enqueue(task)) {
                VikaSpeech.say(laura, player, "task.queued", LineFormatter.values().with("task", task.describe()));
                if (work.queued().size() >= 5) {
                    VikaAdvancements.award(player, "queue_5");
                }
            } else {
                VikaSpeech.say(laura, player, "task.queue_full", LineFormatter.values());
            }
            return;
        }
        laura.workGoal().reset();
        // A direct order is tried right away, even when the last one of its kind found nothing.
        laura.workGoal().forgetFailures(type);
        laura.wakeUp();
        laura.setOrderedToSit(false);
        work.replaceCurrent(task);
        VikaSpeech.say(laura, player, "task.start." + type.key(), LineFormatter.values());
    }

    public static void enableJob(ServerPlayer player, VikaEntity laura, VikaJob job, int radius) {
        if (!VikaConfig.workEnabled.get()) {
            VikaSpeech.say(laura, player, "work.disabled", LineFormatter.values());
            return;
        }
        if (job != VikaJob.COOK && !mobGriefingAllowed(laura)) {
            VikaSpeech.say(laura, player, "work.no_griefing", LineFormatter.values());
            return;
        }
        int r = Math.max(3, Math.min(VikaConfig.workMaxRadius.getInt(), radius > 0 ? radius : VikaConfig.workRadius.getInt()));
        laura.workplace().enableJob(job, new WorkArea(player.blockPosition(), r, player.level().dimension()));
        laura.workGoal().reset();
        laura.wakeUp();
        laura.setMode(VikaMode.WORK);
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 0.3, player.getZ(), 20, r * 0.4, 0.2, r * 0.4, 0.0);
        }
        VikaSpeech.say(laura, player, "job.start." + job.key(), LineFormatter.values().with("radius", r));
        VikaAdvancements.award(player, "job");
    }

    public static void disableJob(ServerPlayer player, VikaEntity laura, @Nullable VikaJob job) {
        if (job == null) {
            laura.workplace().clearJobs();
        } else {
            laura.workplace().disableJob(job);
        }
        laura.workGoal().reset();
        if (!laura.workplace().hasJobs() && laura.getMode() == VikaMode.WORK) {
            laura.setMode(VikaMode.FOLLOW);
        }
        VikaSpeech.say(laura, player, "job.stop", LineFormatter.values());
    }

    public static void backToWork(ServerPlayer player, VikaEntity laura) {
        if (!laura.workplace().hasJobs()) {
            VikaSpeech.say(laura, player, "job.none", LineFormatter.values());
            return;
        }
        laura.wakeUp();
        laura.setMode(VikaMode.WORK);
        VikaSpeech.say(laura, player, "job.resume", LineFormatter.values());
    }

    private static boolean mobGriefingAllowed(VikaEntity laura) {
        return laura.level() instanceof ServerLevel level && level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
    }

    /** Called every 5 ticks: starts the next queued task and runs instant ones. */
    public static void processQueue(VikaEntity laura) {
        VikaWorkplace work = laura.workplace();
        VikaTask current = work.current();
        if (current == null) {
            if (laura.fetchGoal().isActive() || work.queued().isEmpty()) {
                return;
            }
            // An errand whose kind just found nothing to do waits its turn without blocking the others.
            current = work.advance(task -> !laura.workGoal().isWaiting(task));
            if (current == null) {
                return;
            }
            laura.workGoal().reset();
            ServerPlayer owner = VikaSpeech.owner(laura);
            if (current.type() == VikaTask.Type.FETCH) {
                ItemSpec spec = ItemSpec.parse(current.arg()).orElse(null);
                if (owner != null && spec != null) {
                    startFetch(owner, laura, spec, current.count());
                }
                work.finishCurrent();
                return;
            }
            // A new attempt at an errand that already said it found nothing starts without a word.
            if (owner != null && current.type().isErrand() && !laura.workGoal().isQuiet(current)) {
                VikaSpeech.say(laura, owner, "task.start." + current.type().key(), LineFormatter.values());
            }
        }
        if (!current.type().isErrand()) {
            ServerPlayer owner = VikaSpeech.owner(laura);
            work.finishCurrent();
            if (owner == null) {
                return;
            }
            // The queue is saved with her, so it goes on in the entity she becomes in another dimension.
            switch (current.type()) {
                case COME -> come(owner, laura);
                case GO_HOME -> goHome(owner, laura);
                case FOLLOW -> {
                    laura.setMode(VikaMode.FOLLOW);
                    VikaSpeech.say(laura, owner, "order.follow", LineFormatter.values());
                }
                case STAY -> {
                    laura.setMode(VikaMode.STAY);
                    VikaSpeech.say(laura, owner, "order.stay", LineFormatter.values());
                }
                case WORK -> backToWork(owner, laura);
                default -> {
                }
            }
        }
    }

    // ------------------------------------------------------------------ chests

    /** Assigns the container the player is looking at. {@code purpose} null removes the assignment. */
    public static void assignChest(ServerPlayer player, VikaEntity laura, @Nullable ChestPurpose purpose) {
        BlockPos pos = lookedAtContainer(player);
        if (pos == null) {
            VikaSpeech.say(laura, player, "chest.look_at_one", LineFormatter.values());
            return;
        }
        if (purpose == null) {
            if (laura.workplace().unassign(pos, player.level().dimension())) {
                VikaSpeech.say(laura, player, "chest.removed", LineFormatter.values());
            } else {
                VikaSpeech.say(laura, player, "chest.not_assigned", LineFormatter.values());
            }
            return;
        }
        if (com.l7cor1s.viktoria.platform.VikaInventories.isLocked(laura, pos)
                || !com.l7cor1s.viktoria.platform.VikaInventories.playerMayUse(player, player.serverLevel(), pos)) {
            // A chest the player could not open (locked, protected, claimed by someone else) is not hers to use.
            VikaSpeech.say(laura, player, "chest.not_assigned", LineFormatter.values());
            return;
        }
        if (!laura.workplace().assign(pos, player.level().dimension(), purpose)) {
            VikaSpeech.say(laura, player, "chest.too_many", LineFormatter.values());
            return;
        }
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 10, 0.3, 0.2, 0.3, 0.0);
        }
        VikaSpeech.say(laura, player, "chest.assigned", LineFormatter.values().with("purpose", Component.translatable("viktoria.chest." + purpose.key())));
        java.util.Set<ChestPurpose> known = java.util.EnumSet.noneOf(ChestPurpose.class);
        laura.workplace().chests().forEach(c -> known.add(c.purpose()));
        if (known.size() == ChestPurpose.values().length) {
            VikaAdvancements.award(player, "chests_all");
        }
    }

    @Nullable
    public static BlockPos lookedAtContainer(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(6.0));
        BlockHitResult hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockPos pos = hit.getBlockPos();
        return com.l7cor1s.viktoria.platform.VikaInventories.at(player.serverLevel(), pos) != null ? pos.immutable() : null;
    }

    // ------------------------------------------------------------------ affection actions

    public static void emote(ServerPlayer player, VikaEntity laura, Emote emote) {
        if (emote == Emote.NONE || !emote.playerTriggered) {
            VikaSpeech.say(laura, player, "confused", LineFormatter.values());
            return;
        }
        if (isCuddle(emote)) {
            // Asked for as an emote, a hug or a kiss still is one: her rules, the counter and the advancement.
            if (emote == Emote.HUG ? hug(player, laura) : kiss(player, laura)) {
                VikaAdvancements.onEmote(player, emote);
                com.l7cor1s.viktoria.api.VikaAPI.fire("emote", laura, player, emote.animationName());
            }
            return;
        }
        if (laura.isAsleep()) {
            laura.wakeUp();
        }
        laura.playEmote(emote);
        VikaAdvancements.onEmote(player, emote);
        com.l7cor1s.viktoria.api.VikaAPI.fire("emote", laura, player, emote.animationName());
        if (emote == Emote.DANCE) {
            laura.brain().onDance();
            VikaSpeech.say(laura, player, "dance", LineFormatter.values());
        } else {
            laura.brain().needs().add(Needs.Need.FUN, 3);
        }
    }

    /** Returns false when she pushed it away. */
    public static boolean hug(ServerPlayer player, VikaEntity laura) {
        // Hungry, sad or jealous, she takes the hug: that is how her partner comforts her. Only a
        // grudge pushes it away (she sulks or is still angry), and asking again right away gets
        // through, as for any order.
        if (laura.brain().holdsGrudge() && !laura.brain().askedAgain("HUG")) {
            VikaSpeech.say(laura, player, "hug.refused", LineFormatter.values());
            laura.playEmote(Emote.POUT);
            return false;
        }
        laura.getLookControl().setLookAt(player);
        laura.playEmote(Emote.HUG);
        laura.brain().onHug(player);
        VikaSpeech.say(laura, player, "hug", LineFormatter.values());
        VikaAdvancements.add(player, "hugs", 1);
        return true;
    }

    /** Returns false when she did not kiss back. */
    public static boolean kiss(ServerPlayer player, VikaEntity laura) {
        if (laura.isGagged()) {
            VikaSpeech.say(laura, player, "gagged_talk", LineFormatter.values());
            return false;
        }
        // By design a kiss has to be earned: no grudge, and enough affection. Her needs do not matter.
        if (laura.brain().holdsGrudge() || laura.getAffection() < 200) {
            VikaSpeech.say(laura, player, "kiss.refused", LineFormatter.values());
            laura.playEmote(Emote.SLAP);
            return false;
        }
        laura.getLookControl().setLookAt(player);
        laura.playEmote(Emote.KISS);
        laura.brain().onKiss(player);
        VikaSpeech.say(laura, player, "kiss", LineFormatter.values());
        VikaAdvancements.add(player, "kisses", 1);
        return true;
    }

    public static void rename(ServerPlayer player, VikaEntity laura, String name) {
        String clean = name.replaceAll("[\\p{Cntrl}§]", "").trim();
        if (clean.isEmpty() || clean.length() > 32) {
            VikaSpeech.say(laura, player, "rename.invalid", LineFormatter.values());
            return;
        }
        laura.setCustomName(Component.literal(clean));
        VikaSpeech.say(laura, player, "rename.done", LineFormatter.values().with("name", clean));
        VikaAdvancements.award(player, "rename");
    }

    // ------------------------------------------------------------------ wash

    /** A bucket of water poured over her: a quick wash, whether she went looking for water or not. */
    public static void wash(ServerPlayer player, VikaEntity laura, InteractionHand hand) {
        if (!player.getAbilities().instabuild) {
            player.setItemInHand(hand, new ItemStack(net.minecraft.world.item.Items.BUCKET));
        }
        laura.wakeUp();
        laura.brain().needs().add(Needs.Need.HYGIENE, 60);
        laura.playSound(SoundEvents.BUCKET_EMPTY, 1.0F, 1.0F);
        laura.spawnParticles(ParticleTypes.SPLASH, 40, 0.4);
        laura.spawnParticles(ParticleTypes.FALLING_WATER, 12, 0.3);
        laura.brain().onWashed(player);
        laura.brain().syncToEntity();
    }

    // ------------------------------------------------------------------ gag

    public static void gag(ServerPlayer player, VikaEntity laura, ItemStack stack) {
        if (laura.isGagged()) {
            VikaSpeech.say(laura, player, "gagged_talk", LineFormatter.values());
            return;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        laura.setGagged(true, VikaConfig.gagSeconds.getInt() * 20);
        laura.brain().changeAffection(-VikaConfig.gagAffectionPenalty.getInt());
        laura.brain().needs().add(Needs.Need.FUN, -10);
        laura.playSound(SoundEvents.GRASS_PLACE, 1.0F, 0.8F);
        laura.playLauraSound(VikaRegistries.Sound.MUFFLED, 1.0F, 1.0F);
        laura.playEmote(Emote.STOMP);
        laura.spawnParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.HAY_BLOCK.defaultBlockState()), 12, 0.3);
        VikaSpeech.say(laura, player, "gag.on", LineFormatter.values());
        VikaAdvancements.award(player, "gag");
    }

    /** Removes the gag. {@code self}: she took it off herself after the timer. */
    public static void ungag(@Nullable ServerPlayer player, VikaEntity laura, boolean self) {
        if (!laura.isGagged()) {
            return;
        }
        laura.setGagged(false, 0);
        laura.playSound(SoundEvents.GRASS_BREAK, 1.0F, 1.0F);
        laura.spawnItemParticles(new ItemStack(net.minecraft.world.item.Items.WHEAT), 10);
        ServerPlayer target = player != null ? player : VikaSpeech.owner(laura);
        if (target != null) {
            VikaSpeech.say(laura, target, self ? "gag.off_self" : "gag.off", LineFormatter.values());
        }
        if (!self && player != null) {
            laura.brain().changeAffection(2);
        } else {
            laura.brain().makeHappy(0);
            laura.playEmote(Emote.STOMP);
        }
    }

    // ------------------------------------------------------------------ items

    public static void giveItem(ServerPlayer player, VikaEntity laura, ItemStack stack, InteractionHand hand) {
        if (VikaBags.isWearableOnBack(stack) && laura.getBackItem().isEmpty()) {
            laura.setBackItem(stack.copyWithCount(1));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            laura.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0F, 1.0F);
            laura.playEmote(Emote.TWIRL);
            laura.brain().changeAffection(5);
            VikaSpeech.say(laura, player, laura.bags().hasBackpack() ? "backpack.equip" : "backpack.equip_small", LineFormatter.values().with("item", stack.getHoverName()));
            VikaAdvancements.award(player, "backpack");
            return;
        }
        if (GiftTable.food(stack) == null && ViktoriaMod.platform().equipTrinket(laura, stack, false)) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            laura.playSound(SoundEvents.ARMOR_EQUIP_GOLD.value(), 1.0F, 1.2F);
            laura.playEmote(Emote.BLUSH);
            laura.hearts(4);
            laura.brain().changeAffection(8);
            VikaSpeech.say(laura, player, "trinket.equip", LineFormatter.values().with("item", stack.getHoverName()));
            VikaAdvancements.award(player, "curio");
            return;
        }
        GiftTable.FoodInfo food = GiftTable.food(stack);
        boolean hungry = VikaConfig.needsEnabled.get() && laura.brain().needs().get(Needs.Need.HUNGER) < 90;
        ItemSpec wanted = laura.brain().desiredItem();
        boolean desiredFood = wanted != null && wanted.test(stack) && laura.brain().desire().mustEat();
        boolean isRaw = player.level() instanceof ServerLevel level && CookingSupport.isRawCookable(level, stack);
        if (food != null && (hungry || desiredFood || food.preference() == GiftTable.Preference.FAVORITE) && !(isRaw && !desiredFood && laura.brain().needs().get(Needs.Need.HUNGER) > 20)) {
            if (laura.brain().eat(stack, player)) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return;
        }
        if (laura.brain().receiveGift(player, stack)) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return;
        }
        if (food != null) {
            // Food she does not need right now goes to her pantry (her inventory).
            ItemStack rest = laura.bags().add(stack.copyWithCount(1));
            if (rest.isEmpty()) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                VikaSpeech.say(laura, player, "eat.later", LineFormatter.values().with("item", stack.getHoverName()));
            } else {
                VikaSpeech.say(laura, player, "inventory_full", LineFormatter.values());
            }
        }
    }

    private static void eatBestFood(ServerPlayer player, VikaEntity laura) {
        var inventory = laura.inventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            GiftTable.FoodInfo food = stack.isEmpty() ? null : GiftTable.food(stack);
            if (food != null && food.preference() != GiftTable.Preference.DISLIKED && laura.brain().eat(stack, null)) {
                stack.shrink(1);
                inventory.setChanged();
                return;
            }
        }
        VikaSpeech.say(laura, player, "need.hunger.no_food", LineFormatter.values());
    }

    // ------------------------------------------------------------------ info

    public static void sendInfo(ServerPlayer player, VikaEntity laura) {
        MutableComponent header = Component.literal("=== ").append(VikaSpeech.nameComponent(laura)).append(" ===").withStyle(ChatFormatting.LIGHT_PURPLE);
        player.sendSystemMessage(header);
        player.sendSystemMessage(line("viktoria.info.mood", Component.translatable("viktoria.mood." + laura.getMood().key())));
        player.sendSystemMessage(line("viktoria.info.relation", Component.translatable("viktoria.relation." + laura.brain().relationKey()), laura.getAffection()));
        player.sendSystemMessage(line("viktoria.info.health", Math.round(laura.getHealth()), Math.round(laura.getMaxHealth())));
        if (VikaConfig.needsEnabled.get()) {
            MutableComponent needs = Component.empty();
            for (Needs.Need need : Needs.Need.values()) {
                int v = Math.round(laura.brain().needs().get(need));
                ChatFormatting color = v < 20 ? ChatFormatting.RED : v < 50 ? ChatFormatting.GOLD : ChatFormatting.GREEN;
                needs.append(Component.translatable("viktoria.need." + need.key())).append(Component.literal(" " + v + "%  ").withStyle(color));
            }
            player.sendSystemMessage(needs);
        }
        if (laura.brain().desire() != null) {
            long seconds = Math.max(0, (laura.brain().desire().deadline() - laura.level().getGameTime()) / 20);
            player.sendSystemMessage(line("viktoria.info.desire", laura.brain().desire().describe(), seconds / 60, seconds % 60));
        }
        player.sendSystemMessage(line("viktoria.info.mode", Component.translatable("viktoria.mode." + laura.getMode().key()), Component.translatable("viktoria.combat." + laura.getCombatMode().key())));
        if (!laura.workplace().jobs().isEmpty()) {
            MutableComponent jobs = Component.empty();
            laura.workplace().jobs().forEach((job, area) -> jobs.append(Component.translatable("viktoria.job." + job.key())).append(" (" + area.describe() + ")  "));
            player.sendSystemMessage(line("viktoria.info.jobs", jobs));
        }
        if (laura.workplace().current() != null || !laura.workplace().queued().isEmpty()) {
            MutableComponent tasks = Component.empty();
            if (laura.workplace().current() != null) {
                tasks.append(laura.workplace().current().describe().copy().withStyle(ChatFormatting.YELLOW)).append("  ");
            }
            for (VikaTask t : laura.workplace().queued()) {
                tasks.append(t.describe()).append("  ");
            }
            player.sendSystemMessage(line("viktoria.info.tasks", tasks));
        }
        if (laura.getHomePos() != null) {
            BlockPos h = laura.getHomePos();
            player.sendSystemMessage(line("viktoria.info.home", h.getX() + " " + h.getY() + " " + h.getZ()));
        }
        player.sendSystemMessage(line("viktoria.info.days", laura.daysTogether()));
    }

    private static MutableComponent line(String key, Object... args) {
        return Component.translatable(key, args).withStyle(ChatFormatting.GRAY);
    }

    /** Teleport used by several orders; kept here to share the particle effect. */
    static void puff(VikaEntity laura) {
        laura.spawnParticles(ParticleTypes.PORTAL, 20, 0.5);
    }

    public static boolean teleportNear(VikaEntity laura, BlockPos pos) {
        boolean ok = VikaMovement.teleportNear(laura, pos);
        if (ok) {
            puff(laura);
        }
        return ok;
    }
}
