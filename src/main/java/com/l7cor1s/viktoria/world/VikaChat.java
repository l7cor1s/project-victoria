package com.l7cor1s.viktoria.world;

import com.l7cor1s.viktoria.config.VikaConfig;
import com.l7cor1s.viktoria.dialogue.DialogueManager;
import com.l7cor1s.viktoria.dialogue.LineFormatter;
import com.l7cor1s.viktoria.dialogue.TextMatcher;
import com.l7cor1s.viktoria.entity.Emote;
import com.l7cor1s.viktoria.entity.VikaEntity;
import com.l7cor1s.viktoria.entity.VikaJob;
import com.l7cor1s.viktoria.entity.VikaSpeech;
import com.l7cor1s.viktoria.entity.brain.VikaBrain;
import com.l7cor1s.viktoria.entity.work.ChestPurpose;
import com.l7cor1s.viktoria.entity.work.VikaTask;
import com.l7cor1s.viktoria.gift.GiftTable;
import com.l7cor1s.viktoria.network.VikaAction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Understands what players write in chat, in any language the dialogue files know, and turns it
 * into orders, answers and reactions.
 * <ul>
 *     <li>Several orders can be chained with "then" (or "puis", "danach", "luego"...): the first one
 *     runs now, the others are queued.</li>
 *     <li>With several companions, the message goes to the one whose name is written, to all of them
 *     with "everyone" (or "les filles"...), otherwise to the selected or nearest one.</li>
 * </ul>
 *
 * @author vyrriox
 */
public final class VikaChat {
    private static final Set<String> ANSWERS = Set.of("yes", "no");

    private VikaChat() {
    }

    /** Called by the loader for every chat message. Handling is deferred one tick so her answer comes after the message. */
    public static void onChat(ServerPlayer player, String message) {
        MinecraftServer server = player.getServer();
        if (server == null || message == null || message.isBlank() || message.startsWith("/")) {
            return;
        }
        VikaManager.schedule(server, 1, () -> handle(player, message));
    }

    /** Companions the message is for. */
    static List<VikaEntity> targets(ServerPlayer player, String message) {
        double range = VikaConfig.chatRange.getInt();
        List<VikaEntity> nearby = new ArrayList<>();
        for (VikaEntity laura : VikaManager.findAll(player)) {
            if (laura.level() == player.level() && laura.distanceToSqr(player) <= range * range) {
                nearby.add(laura);
            }
        }
        if (nearby.isEmpty()) {
            return nearby;
        }
        if (DialogueManager.mentionsEveryone(message, player.clientInformation().language())) {
            return nearby;
        }
        List<VikaEntity> named = new ArrayList<>();
        for (VikaEntity laura : nearby) {
            if (DialogueManager.mentions(message, laura.getLauraName())) {
                named.add(laura);
            }
        }
        if (!named.isEmpty()) {
            return named;
        }
        if (VikaConfig.requireNameInChat.get()) {
            return new ArrayList<>();
        }
        VikaEntity preferred = VikaManager.findNear(player, range);
        return preferred == null ? new ArrayList<>() : new ArrayList<>(List.of(preferred));
    }

    private static void handle(ServerPlayer player, String message) {
        if (player.hasDisconnected()) {
            return;
        }
        List<String> segments = DialogueManager.splitOrders(message, player.clientInformation().language());
        // The summoning words work from anywhere; everything else is proximity chat.
        for (String segment : segments) {
            DialogueManager.IntentMatch match = DialogueManager.match(segment, player.clientInformation().language());
            if (match != null && match.intent().equals("summon")) {
                VikaManager.summon(player, true);
                return;
            }
        }
        List<VikaEntity> targets = targets(player, message);
        if (targets.isEmpty()) {
            return;
        }
        for (VikaEntity laura : targets) {
            if (!laura.isOwnedBy(player) && !VikaConfig.othersCanInteract.get()) {
                continue;
            }
            if (laura.isOwnedBy(player)) {
                laura.brain().onChatFromOwner();
            }
            boolean first = true;
            boolean understood = false;
            for (String segment : segments) {
                DialogueManager.IntentMatch match = DialogueManager.match(segment, player.clientInformation().language());
                if (match == null) {
                    continue;
                }
                understood = true;
                // An earlier order of the same message may have sent her to another dimension (her home).
                VikaEntity live = VikaManager.live(laura);
                if (live == null) {
                    break;
                }
                handleIntent(player, live, match, segment, !first);
                first = false;
            }
            if (!understood) {
                notUnderstood(player, laura, message);
            }
        }
        if (targets.size() == 1) {
            VikaManager.select(player, targets.get(0));
        }
    }

    private static void notUnderstood(ServerPlayer player, VikaEntity laura, String message) {
        VikaBrain brain = laura.brain();
        if (brain.pendingQuestion() == VikaBrain.Question.LOVE) {
            DialogueManager.IntentMatch answer = DialogueManager.match(message, ANSWERS, player.clientInformation().language());
            if (answer != null) {
                brain.answer(player, answer.intent().equals("yes"));
                return;
            }
        }
        if (DialogueManager.mentions(message, laura.getLauraName())) {
            VikaSpeech.say(laura, player, "confused", LineFormatter.values());
        } else if (player.getServer().getPlayerCount() > 1 && laura.isOwnedBy(player)) {
            brain.watcher().onChatWithOthers(player);
        }
    }

    private static final java.util.regex.Pattern NUMBER = java.util.regex.Pattern.compile("\\d{1,4}");

    /**
     * The quantity written in a fetch order ("bring me 3 apples", "apporte 32 pains"), from 1 to
     * 576, or 0 when the message has no number (the default amount is then used). Digits only, in
     * any language, including scripts written without spaces.
     */
    static int countIn(TextMatcher.Prepared message) {
        java.util.regex.Matcher m = NUMBER.matcher(message.text());
        return m.find() ? Math.max(1, Math.min(576, Integer.parseInt(m.group()))) : 0;
    }

    private static void handleIntent(ServerPlayer player, VikaEntity laura, DialogueManager.IntentMatch match, String segment, boolean queue) {
        VikaBrain brain = laura.brain();
        String intent = match.intent();
        TextMatcher.Prepared prepared = TextMatcher.Prepared.of(segment);
        LineFormatter.Values none = LineFormatter.values();
        if (com.l7cor1s.viktoria.api.VikaAPI.fire("chat", laura, player, intent)) {
            // A script handled this message itself.
            return;
        }
        switch (intent) {
            case "yes", "no" -> {
                // Only an answer to her question: "no" alone is a filler word in several languages.
                brain.answer(player, intent.equals("yes"));
            }
            case "where" -> VikaSpeech.say(laura, player, "where.here", none);
            case "follow" -> orderOrQueue(player, laura, VikaAction.FOLLOW, VikaTask.Type.FOLLOW, queue);
            case "stay" -> orderOrQueue(player, laura, VikaAction.STAY, VikaTask.Type.STAY, queue);
            case "come" -> orderOrQueue(player, laura, VikaAction.COME, VikaTask.Type.COME, queue);
            case "home" -> orderOrQueue(player, laura, VikaAction.HOME, VikaTask.Type.GO_HOME, queue);
            case "wander" -> VikaActions.perform(player, laura, VikaAction.WANDER, "", VikaActions.Source.CHAT);
            case "set_home" -> VikaActions.perform(player, laura, VikaAction.SET_HOME, "", VikaActions.Source.CHAT);
            case "stop" -> VikaActions.perform(player, laura, VikaAction.STOP, "", VikaActions.Source.CHAT);
            case "fetch" -> {
                List<String> items = DialogueManager.matchItems(prepared, player.clientInformation().language());
                if (items.isEmpty() && player.getMainHandItem().isEmpty()) {
                    VikaSpeech.say(laura, player, "fetch.what", none);
                    return;
                }
                VikaActions.fetch(player, laura, items.isEmpty() ? "held" : items.get(0), countIn(prepared), queue);
            }
            case "chop_tree" -> VikaActions.task(player, laura, VikaTask.Type.CHOP_TREE, 0, queue);
            case "harvest" -> VikaActions.task(player, laura, VikaTask.Type.HARVEST, 0, queue);
            case "cook" -> VikaActions.task(player, laura, VikaTask.Type.COOK, 0, queue);
            case "job_lumberjack" -> VikaActions.enableJob(player, laura, VikaJob.LUMBERJACK, 0);
            case "job_farmer" -> VikaActions.enableJob(player, laura, VikaJob.FARMER, 0);
            case "job_cook" -> VikaActions.enableJob(player, laura, VikaJob.COOK, 0);
            case "job_stop" -> VikaActions.disableJob(player, laura, null);
            case "back_to_work" -> orderOrQueue(player, laura, null, VikaTask.Type.WORK, queue);
            case "assign_chest" -> {
                String purpose = DialogueManager.matchChestPurpose(prepared, player.clientInformation().language());
                ChestPurpose p = purpose == null ? null : ChestPurpose.byName(purpose);
                if (p == null) {
                    VikaSpeech.say(laura, player, "chest.which_purpose", none);
                } else {
                    VikaActions.assignChest(player, laura, p);
                }
            }
            case "hug" -> VikaActions.perform(player, laura, VikaAction.HUG, "", VikaActions.Source.CHAT);
            case "kiss" -> VikaActions.perform(player, laura, VikaAction.KISS, "", VikaActions.Source.CHAT);
            case "apology" -> brain.apologize(player);
            case "eat" -> VikaActions.perform(player, laura, VikaAction.EAT, "", VikaActions.Source.CHAT);
            case "sleep" -> VikaActions.perform(player, laura, VikaAction.SLEEP, "", VikaActions.Source.CHAT);
            case "wake_up" -> VikaActions.perform(player, laura, VikaAction.WAKE_UP, "", VikaActions.Source.CHAT);
            case "beautiful", "love_you", "miss_you", "thanks" -> {
                brain.onCompliment();
                VikaSpeech.respond(laura, player, intent, none);
            }
            case "insult" -> {
                brain.onInsult(player);
                VikaSpeech.respond(laura, player, intent, none);
            }
            case "what_do_you_want" -> {
                if (brain.desire() != null) {
                    var d = brain.desire();
                    VikaSpeech.say(laura, player, "desire.tell", LineFormatter.values().with("item", d.describe()).with("place", d.describe()).with("activity", d.describe()));
                } else {
                    VikaSpeech.say(laura, player, "desire.none", none);
                }
            }
            case "marry_me" -> {
                VikaSpeech.respond(laura, player, intent, none);
                VikaAdvancements.award(player, "marry");
            }
            case "how_are_you" -> VikaSpeech.sayFirst(laura, player, none, "status." + laura.getMood().key(), "status");
            case "player_hungry" -> {
                if (!giveFood(player, laura)) {
                    VikaSpeech.respond(laura, player, intent, none);
                }
            }
            default -> {
                if (intent.startsWith("emote_")) {
                    VikaActions.emote(player, laura, Emote.byName(intent.substring("emote_".length())));
                    VikaSpeech.respond(laura, player, intent, none);
                } else if (!VikaSpeech.respond(laura, player, intent, none)) {
                    VikaSpeech.say(laura, player, "confused", none);
                }
            }
        }
    }

    private static void orderOrQueue(ServerPlayer player, VikaEntity laura, VikaAction action, VikaTask.Type type, boolean queue) {
        if (queue && (laura.workplace().current() != null || laura.fetchGoal().isActive())) {
            VikaTask task = new VikaTask(type, "", 0, null);
            if (laura.workplace().enqueue(task)) {
                VikaSpeech.say(laura, player, "task.queued", LineFormatter.values().with("task", task.describe()));
            }
            return;
        }
        if (action == null) {
            VikaActions.backToWork(player, laura);
        } else {
            VikaActions.perform(player, laura, action, "", VikaActions.Source.CHAT);
        }
    }

    /** "I'm hungry": she gives her partner some of her food. */
    private static boolean giveFood(ServerPlayer player, VikaEntity laura) {
        var inv = laura.inventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            GiftTable.FoodInfo food = stack.isEmpty() ? null : GiftTable.food(stack);
            if (food != null && food.preference() != GiftTable.Preference.DISLIKED) {
                ItemStack given = stack.split(1);
                inv.setChanged();
                laura.brain().giveToPlayer(player, given);
                VikaSpeech.say(laura, player, "owner.feed", LineFormatter.values().with("item", given.getHoverName()));
                return true;
            }
        }
        return false;
    }
}
