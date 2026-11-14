package com.l7cor1s.viktoria.gift;

import com.l7cor1s.viktoria.relationship.MoodType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class GiftProfile {
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

    public int affinityDelta(ItemStack stack) {
        Item item = stack.getItem();
        if (hated.contains(item)) {
            return -8;
        }
        if (loved.contains(item)) {
            return 12;
        }
        if (liked.contains(item) || matchesLikedTag(stack)) {
            return 5;
        }
        return 1;
    }

    public MoodType moodAfterGift(ItemStack stack) {
        Item item = stack.getItem();
        if (hated.contains(item)) {
            return MoodType.IRRITATION;
        }
        if (loved.contains(item)) {
            return MoodType.JOY;
        }
        if (liked.contains(item) || matchesLikedTag(stack)) {
            return MoodType.AFFECTION;
        }
        return MoodType.NEUTRAL;
    }

    public Optional<String> reactionKey(ItemStack stack, int affinity) {
        Item item = stack.getItem();
        if (hated.contains(item)) {
            return Optional.of("viktoria.gift.hated");
        }
        if (loved.contains(item)) {
            return Optional.of(affinity >= 60 ? "viktoria.gift.loved_close" : "viktoria.gift.loved");
        }
        if (liked.contains(item) || matchesLikedTag(stack)) {
            return Optional.of("viktoria.gift.liked");
        }
        return Optional.of("viktoria.gift.neutral");
    }

    private boolean matchesLikedTag(ItemStack stack) {
        for (TagKey<Item> tag : likedTags) {
            if (stack.is(tag)) {
                return true;
            }
        }
        return false;
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
