package com.l7cor1s.viktoria.dialogue;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.l7cor1s.viktoria.ViktoriaMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads {@code data/<ns>/dialogue/<character_id>.json} overlays onto built-in trees.
 */
public final class DialogueLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();

    public DialogueLoader() {
        super(GSON, "dialogue");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<String, Map<String, DialogueNode>> trees = new HashMap<>();
        // Start from builtins so partial JSON can patch.
        DialogueTrees.builtinTree("viktoria").forEach((id, node) ->
                trees.computeIfAbsent("viktoria", k -> new HashMap<>()).put(id, node));

        for (Map.Entry<ResourceLocation, JsonElement> entry : prepared.entrySet()) {
            ResourceLocation id = entry.getKey();
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject root = entry.getValue().getAsJsonObject();
            String character = root.has("character") ? root.get("character").getAsString() : id.getPath();
            if (!root.has("nodes") || !root.get("nodes").isJsonObject()) {
                ViktoriaMod.LOGGER.warn("Dialogue file {} missing nodes object", id);
                continue;
            }
            Map<String, DialogueNode> tree = trees.computeIfAbsent(character, k -> new HashMap<>());
            JsonObject nodes = root.getAsJsonObject("nodes");
            for (Map.Entry<String, JsonElement> nodeEntry : nodes.entrySet()) {
                DialogueNode parsed = parseNode(nodeEntry.getKey(), nodeEntry.getValue().getAsJsonObject());
                tree.put(parsed.id(), parsed);
            }
            ViktoriaMod.LOGGER.info("Loaded dialogue overlay {} for character {}", id, character);
        }
        DialogueTrees.replaceLoaded(trees);
    }

    private static DialogueNode parseNode(String id, JsonObject json) {
        String text = json.has("text") ? json.get("text").getAsString() : "viktoria.dialogue." + id;
        Map<String, String> moodText = new HashMap<>();
        if (json.has("mood_text") && json.get("mood_text").isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("mood_text").entrySet()) {
                moodText.put(e.getKey(), e.getValue().getAsString());
            }
        }
        int minAffinity = json.has("min_affinity") ? json.get("min_affinity").getAsInt() : 0;
        String requiredMood = json.has("required_mood") ? json.get("required_mood").getAsString() : null;
        List<DialogueChoice> choices = new ArrayList<>();
        if (json.has("choices") && json.get("choices").isJsonArray()) {
            JsonArray arr = json.getAsJsonArray("choices");
            for (JsonElement el : arr) {
                choices.add(parseChoice(el.getAsJsonObject()));
            }
        }
        return new DialogueNode(id, text, moodText, choices, minAffinity, requiredMood);
    }

    private static DialogueChoice parseChoice(JsonObject json) {
        String id = json.get("id").getAsString();
        String lang = json.get("lang").getAsString();
        String next = json.has("next") ? json.get("next").getAsString() : null;
        int affinity = json.has("affinity") ? json.get("affinity").getAsInt() : 0;
        String mood = json.has("mood") ? json.get("mood").getAsString() : null;
        String action = json.has("action") ? json.get("action").getAsString() : null;
        int minAffinity = json.has("min_affinity") ? json.get("min_affinity").getAsInt() : 0;
        List<String> requiredMoods = new ArrayList<>();
        if (json.has("required_moods") && json.get("required_moods").isJsonArray()) {
            json.getAsJsonArray("required_moods").forEach(e -> requiredMoods.add(e.getAsString()));
        }
        return new DialogueChoice(id, lang, next, affinity, mood, action, minAffinity, requiredMoods);
    }
}
