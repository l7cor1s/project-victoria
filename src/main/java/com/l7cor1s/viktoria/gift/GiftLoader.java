package com.l7cor1s.viktoria.gift;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.l7cor1s.viktoria.ViktoriaMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;

/** Loads {@code data/<ns>/gifts/<character>.json} into {@link GiftProfiles}. */
public final class GiftLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();

    public GiftLoader() {
        super(GSON, "gifts");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<String, GiftProfile> loaded = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : prepared.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject root = entry.getValue().getAsJsonObject();
            String character = root.has("character") ? root.get("character").getAsString() : entry.getKey().getPath();
            GiftProfile.Builder builder = GiftProfile.builder();
            addItems(builder, root, "loved", true, false, false);
            addItems(builder, root, "liked", false, true, false);
            addItems(builder, root, "hated", false, false, true);
            if (root.has("liked_tags") && root.get("liked_tags").isJsonArray()) {
                for (JsonElement el : root.getAsJsonArray("liked_tags")) {
                    ResourceLocation tagId = ResourceLocation.parse(el.getAsString());
                    builder.likedTag(TagKey.create(BuiltInRegistries.ITEM.key(), tagId));
                }
            }
            loaded.put(character, builder.build());
            ViktoriaMod.LOGGER.info("Loaded gift profile {} for {}", entry.getKey(), character);
        }
        GiftProfiles.replaceLoaded(loaded);
    }

    private static void addItems(GiftProfile.Builder builder, JsonObject root, String key,
                                 boolean loved, boolean liked, boolean hated) {
        if (!root.has(key) || !root.get(key).isJsonArray()) {
            return;
        }
        JsonArray arr = root.getAsJsonArray(key);
        for (JsonElement el : arr) {
            ResourceLocation id = ResourceLocation.parse(el.getAsString());
            Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
            if (item == null || item == net.minecraft.world.item.Items.AIR) {
                ViktoriaMod.LOGGER.warn("Unknown gift item {} in {}", id, key);
                continue;
            }
            if (loved) {
                builder.loved(item);
            } else if (liked) {
                builder.liked(item);
            } else if (hated) {
                builder.hated(item);
            }
        }
    }
}
