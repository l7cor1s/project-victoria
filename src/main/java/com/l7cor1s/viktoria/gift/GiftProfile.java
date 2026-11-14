package com.l7cor1s.viktoria.gift;

import com.l7cor1s.viktoria.compat.IntegrationReactions;
import com.l7cor1s.viktoria.relationship.MoodType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class GiftProfile {
    public enum GiftKind {
        FLOWER, SWEET, FOOD, ORE_HIGH, ORE_MID, HATED, LOVED, LIKED, NEUTRAL
    }

    private final Set<Item> loved;
    private final Set<Item> liked;
    private final Set<Item> hated;
    private final List<TagKey<Item>> likedTags;

    private GiftProfile(Set<Item> loved, Set<Item> liked, Set<Item> hated, List<TagKey<Item>> likedTags) {
        this.loved = loved;
        this.liked = liked;
        this.hated = hated;
        this.likedTags = likedTags;
    }

    public static Builder builder() {
        return new Builder();
    }

    public GiftKind classify(ItemStack stack) {
        Item item = stack.getItem();
        if (hated.contains(item)) {
            return GiftKind.HATED;
        }
        if (isOreHigh(item)) {
            return GiftKind.ORE_HIGH;
        }
        if (isOreMid(item)) {
            return GiftKind.ORE_MID;
        }
        if (isSweet(stack)) {
            return GiftKind.SWEET;
        }
        if (stack.is(ItemTags.FLOWERS) || item == Items.PINK_PETALS) {
            return GiftKind.FLOWER;
        }
        if (isEdible(stack) || IntegrationReactions.isSpecialFood(stack)) {
            return GiftKind.FOOD;
        }
        if (loved.contains(item)) {
            return GiftKind.LOVED;
        }
        if (liked.contains(item) || matchesLikedTag(stack)) {
            return GiftKind.LIKED;
        }
        return GiftKind.NEUTRAL;
    }

    public int affinityDelta(ItemStack stack) {
        return switch (classify(stack)) {
            case HATED -> -5;
            case FLOWER -> 5;
            case SWEET, FOOD -> 8;
            case ORE_HIGH -> 6;
            case ORE_MID -> 3;
            case LOVED -> 8;
            case LIKED -> 5;
            case NEUTRAL -> 1;
        };
    }

    public MoodType moodAfterGift(ItemStack stack) {
        return switch (classify(stack)) {
            case HATED -> MoodType.IRRITATION;
            case FLOWER, SWEET, FOOD, LOVED -> MoodType.JOY;
            case ORE_HIGH, ORE_MID, LIKED -> MoodType.AFFECTION;
            case NEUTRAL -> MoodType.NEUTRAL;
        };
    }

    public Optional<String> reactionKey(ItemStack stack, int affinity) {
        return Optional.of(switch (classify(stack)) {
            case HATED -> "viktoria.gift.hated";
            case FLOWER -> "viktoria.gift.flower";
            case SWEET -> "viktoria.gift.sweet";
            case FOOD -> "viktoria.gift.food";
            case ORE_HIGH -> "viktoria.gift.ore_high";
            case ORE_MID -> "viktoria.gift.ore_mid";
            case LOVED -> affinity >= 60 ? "viktoria.gift.loved_close" : "viktoria.gift.loved";
            case LIKED -> "viktoria.gift.liked";
            case NEUTRAL -> "viktoria.gift.neutral";
        });
    }

    private static boolean isSweet(ItemStack stack) {
        Item item = stack.getItem();
        return item == Items.CAKE
                || item == Items.COOKIE
                || item == Items.PUMPKIN_PIE
                || item == Items.HONEY_BOTTLE
                || item == Items.SWEET_BERRIES
                || item == Items.GOLDEN_APPLE
                || item == Items.ENCHANTED_GOLDEN_APPLE
                || item == Items.GLOW_BERRIES
                || item == Items.SUGAR;
    }

    private static boolean isEdible(ItemStack stack) {
        return stack.get(DataComponents.FOOD) != null;
    }

    private static boolean isOreHigh(Item item) {
        return item == Items.DIAMOND
                || item == Items.DIAMOND_BLOCK
                || item == Items.EMERALD
                || item == Items.EMERALD_BLOCK
                || item == Items.NETHERITE_INGOT
                || item == Items.NETHERITE_SCRAP;
    }

    private static boolean isOreMid(Item item) {
        return item == Items.IRON_INGOT
                || item == Items.IRON_BLOCK
                || item == Items.GOLD_INGOT
                || item == Items.GOLD_BLOCK
                || item == Items.COPPER_INGOT
                || item == Items.LAPIS_LAZULI
                || item == Items.AMETHYST_SHARD
                || item == Items.RAW_IRON
                || item == Items.RAW_GOLD;
    }

    private boolean matchesLikedTag(ItemStack stack) {
        for (TagKey<Item> tag : likedTags) {
            if (stack.is(tag)) {
                return true;
            }
        }
        return false;
    }

    public String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    public static final class Builder {
        private final Set<Item> loved = new HashSet<>();
        private final Set<Item> liked = new HashSet<>();
        private final Set<Item> hated = new HashSet<>();
        private final List<TagKey<Item>> likedTags = new ArrayList<>();

        public Builder loved(Item... items) {
            for (Item item : items) {
                loved.add(item);
            }
            return this;
        }

        public Builder liked(Item... items) {
            for (Item item : items) {
                liked.add(item);
            }
            return this;
        }

        public Builder hated(Item... items) {
            for (Item item : items) {
                hated.add(item);
            }
            return this;
        }

        public Builder likedTag(TagKey<Item> tag) {
            likedTags.add(tag);
            return this;
        }

        public GiftProfile build() {
            return new GiftProfile(loved, liked, hated, likedTags);
        }
    }
}
