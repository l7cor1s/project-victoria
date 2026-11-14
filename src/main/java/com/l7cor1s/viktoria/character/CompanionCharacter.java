package com.l7cor1s.viktoria.character;

import com.l7cor1s.viktoria.gift.GiftProfile;
import com.l7cor1s.viktoria.intimate.IntimateHooks;
import com.l7cor1s.viktoria.quest.QuestDefinition;
import com.l7cor1s.viktoria.relationship.AffinityTier;
import com.l7cor1s.viktoria.relationship.MoodType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * Extensible companion definition. Register one instance per character.
 * Dialogue/gifts/quests are data-driven where practical; hooks stay in code.
 */
public abstract class CompanionCharacter {
    private final String id;
    private final Component displayName;

    protected CompanionCharacter(String id, Component displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public Component displayName() {
        return displayName;
    }

    public ResourceLocation dialogueRoot() {
        return ResourceLocation.fromNamespaceAndPath("viktoria", "dialogue/" + id + "/greeting");
    }

    public abstract List<MoodType> supportedMoods();

    public abstract AffinityTier[] affinityTiers();

    public abstract GiftProfile giftProfile();

    public abstract List<QuestDefinition> quests();

    public abstract IntimateHooks intimateHooks();

    /** Home / spawn flavour text key. */
    public String homeClaimLangKey() {
        return "viktoria.home.claim." + id;
    }

    public Optional<String> reactToGift(ItemStack stack, int affinity) {
        return giftProfile().reactionKey(stack, affinity);
    }

    public int giftAffinityDelta(ItemStack stack) {
        return giftProfile().affinityDelta(stack);
    }

    public MoodType giftMoodDelta(ItemStack stack) {
        return giftProfile().moodAfterGift(stack);
    }
}
