package com.l7cor1s.viktoria.api;

import com.l7cor1s.viktoria.ViktoriaCore;
import com.l7cor1s.viktoria.desire.Desire;
import com.l7cor1s.viktoria.desire.DesireType;
import com.l7cor1s.viktoria.dialogue.LineFormatter;
import com.l7cor1s.viktoria.entity.Emote;
import com.l7cor1s.viktoria.entity.VikaEntity;
import com.l7cor1s.viktoria.entity.VikaSpeech;
import com.l7cor1s.viktoria.entity.brain.Needs;
import com.l7cor1s.viktoria.network.VikaAction;
import com.l7cor1s.viktoria.world.VikaActions;
import com.l7cor1s.viktoria.world.VikaManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Public API for other mods and scripts (KubeJS can call it: the package is allowed in
 * {@code kubejs.classfilter.txt}). Every method must be called on the server thread.
 * <p>
 * KubeJS example (server script):
 * <pre>
 * const VikaAPI = Java.loadClass('com.l7cor1s.viktoria.api.VikaAPI')
 * VikaAPI.listen('summon', (laura, player, detail) =&gt; VikaAPI.say(laura, player, 'Hello from KubeJS!'))
 * </pre>
 *
 * @author vyrriox
 */
public final class VikaAPI {
    /** Events: summon, death, revive, desire_fulfilled, desire_failed, gift, emote, chat. */
    @FunctionalInterface
    public interface Listener {
        void handle(VikaEntity laura, @Nullable ServerPlayer player, String detail);
    }

    /** Bridge to a scripting mod's own event system (the KubeJS plugin sets it). Returns true to cancel. */
    @FunctionalInterface
    public interface ExternalEvents {
        boolean post(String event, VikaEntity laura, @Nullable ServerPlayer player, String detail);
    }

    private static final Map<String, List<Listener>> LISTENERS = new ConcurrentHashMap<>();
    private static volatile ExternalEvents external;

    private VikaAPI() {
    }

    // ------------------------------------------------------------------ events

    /** Registers a listener for an event name (see {@link Listener}). */
    public static void listen(String event, Listener listener) {
        LISTENERS.computeIfAbsent(event.toLowerCase(Locale.ROOT), k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public static void setExternalEvents(ExternalEvents events) {
        external = events;
    }

    /** Removes the listeners registered through {@link #listen} (before scripts run again). */
    public static void clearListeners() {
        LISTENERS.clear();
    }

    /**
     * Called by the mod when something happens. A failing listener never breaks the game. Returns
     * true when a script cancelled the event ("chat" cancels her default answer).
     */
    public static boolean fire(String event, VikaEntity laura, @Nullable ServerPlayer player, String detail) {
        String d = detail == null ? "" : detail;
        List<Listener> list = LISTENERS.get(event);
        if (list != null) {
            for (Listener listener : list) {
                try {
                    listener.handle(laura, player, d);
                } catch (Throwable t) {
                    ViktoriaCore.LOGGER.error("A Laura API listener for '{}' failed", event, t);
                }
            }
        }
        ExternalEvents ext = external;
        if (ext != null) {
            try {
                return ext.post(event, laura, player, d);
            } catch (Throwable t) {
                ViktoriaCore.LOGGER.error("A scripted Laura event '{}' failed", event, t);
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ content (same format as the config files)

    /** A gift entry, like in gifts.json: { match, affection, fun, tier, returnChance, food }. */
    public static void addGift(Object entry) {
        ScriptData.add("gifts.gifts", ScriptData.toJson(entry));
        ScriptData.requestReload();
    }

    public static void addFavoriteFood(String item) {
        ScriptData.add("gifts.foods.favorites", ScriptData.toJson(item));
        ScriptData.requestReload();
    }

    public static void addDislikedFood(String item) {
        ScriptData.add("gifts.foods.disliked", ScriptData.toJson(item));
        ScriptData.requestReload();
    }

    /** Something she may give back, like in gifts.json: { item, count, weight }. */
    public static void addReturnGift(Object entry) {
        ScriptData.add("gifts.returnGifts", ScriptData.toJson(entry));
        ScriptData.requestReload();
    }

    /** An item she may wish for, like in desires.json: { item, eat, weight }. */
    public static void addDesiredItem(Object entry) {
        ScriptData.add("desires.items", ScriptData.toJson(entry));
        ScriptData.requestReload();
    }

    /** A place she may dream of, like in desires.json: { id, biomes, icon, weight }. */
    public static void addDesiredPlace(Object entry) {
        ScriptData.add("desires.places", ScriptData.toJson(entry));
        ScriptData.requestReload();
    }

    /** A meal of her cookbook, like in recipes.json: { id, result, count, ingredients, returns }. */
    public static void addMeal(Object entry) {
        ScriptData.add("recipes.meals", ScriptData.toJson(entry));
        ScriptData.requestReload();
    }

    /** Dialogues for a language, like a file of config/viktoria/dialogues: { lines, intents, items, chests }. */
    public static void addDialogue(String locale, Object json) {
        if (ScriptData.toJson(json) instanceof com.google.gson.JsonObject o) {
            ScriptData.addDialogue(locale.toLowerCase(Locale.ROOT), o);
            ScriptData.requestReload();
        }
    }

    /** One more line she can say for a dialogue key ("ambient", "hug", "summon"...). */
    public static void addLine(String locale, String key, String text) {
        com.google.gson.JsonObject lines = new com.google.gson.JsonObject();
        com.google.gson.JsonArray array = new com.google.gson.JsonArray();
        array.add(text);
        lines.add(key, array);
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        root.add("lines", lines);
        addDialogue(locale, root);
    }

    /**
     * One more spelling rule for a language: {@code from} is read as {@code to} when matching chat
     * ("jtm" as "je t aime", "tu est" as "tu es"). An empty {@code to} drops a filler word.
     */
    public static void addSpelling(String locale, String from, String to) {
        com.google.gson.JsonObject spelling = new com.google.gson.JsonObject();
        spelling.addProperty(from, to == null ? "" : to);
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        root.add("spelling", spelling);
        addDialogue(locale, root);
    }

    /** One more chat phrase for an intent, with an optional answer (empty for none). Custom intents work too. */
    public static void addChat(String locale, String intent, String trigger, String response) {
        com.google.gson.JsonObject entry = new com.google.gson.JsonObject();
        com.google.gson.JsonArray triggers = new com.google.gson.JsonArray();
        triggers.add(trigger);
        entry.add("triggers", triggers);
        com.google.gson.JsonArray responses = new com.google.gson.JsonArray();
        if (response != null && !response.isBlank()) {
            responses.add(response);
        }
        entry.add("responses", responses);
        com.google.gson.JsonObject intents = new com.google.gson.JsonObject();
        intents.add(intent, entry);
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        root.add("intents", intents);
        addDialogue(locale, root);
    }

    /** Reloads Laura's config, dialogues and tables on the next server tick. */
    public static void reload() {
        ScriptData.requestReload();
    }

    // ------------------------------------------------------------------ queries

    /** The companions of a player that are loaded in the world. */
    public static List<VikaEntity> companions(ServerPlayer player) {
        return new ArrayList<>(VikaManager.findAll(player));
    }

    /** The companion of the player that answers commands (the selected one or the nearest). */
    @Nullable
    public static VikaEntity companion(ServerPlayer player) {
        return VikaManager.find(player);
    }

    public static String name(VikaEntity laura) {
        return laura.getLauraName();
    }

    public static String mood(VikaEntity laura) {
        return laura.getMood().key();
    }

    public static int affection(VikaEntity laura) {
        return laura.getAffection();
    }

    /** Need by name (hunger, energy, fun, attention, hygiene), 0 to 100. */
    public static float need(VikaEntity laura, String need) {
        return laura.brain().needs().get(Needs.Need.valueOf(need.toUpperCase(Locale.ROOT)));
    }

    // ------------------------------------------------------------------ actions

    /** Summons a companion for the player, like the chat or /laura summon. */
    public static void summon(ServerPlayer player) {
        VikaManager.summon(player, false);
    }

    /** She says a text to a player (with her name, in proximity chat and her speech bubble). */
    public static void say(VikaEntity laura, ServerPlayer player, String text) {
        VikaSpeech.sayRaw(laura, player, Component.literal(text));
    }

    /** She says a dialogue line by key, in the player's language. Returns false if the key does not exist. */
    public static boolean sayLine(VikaEntity laura, ServerPlayer player, String key) {
        return VikaSpeech.say(laura, player, key, LineFormatter.values());
    }

    public static void addAffection(VikaEntity laura, int amount) {
        laura.brain().changeAffection(amount);
    }

    public static void setNeed(VikaEntity laura, String need, float value) {
        laura.brain().needs().set(Needs.Need.valueOf(need.toUpperCase(Locale.ROOT)), Math.max(0, Math.min(100, value)));
    }

    /** Plays an emote by name (wave, clap, dance, ...). */
    public static void emote(VikaEntity laura, String emote) {
        laura.playEmote(Emote.byName(emote));
    }

    /** Gives an order as the player would from the menu: FOLLOW, STAY, COME, FETCH ("minecraft:bread|16")... */
    public static void order(ServerPlayer player, VikaEntity laura, String action, String argument) {
        VikaActions.perform(player, laura, VikaAction.valueOf(action.toUpperCase(Locale.ROOT)), argument == null ? "" : argument, VikaActions.Source.COMMAND);
    }

    /** Gives her a wish for an item (id or #tag) that must be fulfilled within the given minutes. */
    public static void wish(VikaEntity laura, String item, int minutes) {
        long now = laura.level().getGameTime();
        laura.brain().forceDesire(new Desire(DesireType.Kind.ITEM, item, false, now, now + 1200L * Math.max(1, minutes)));
    }
}
