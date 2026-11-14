package com.l7cor1s.viktoria.character;

import com.l7cor1s.viktoria.gift.GiftProfile;
import com.l7cor1s.viktoria.intimate.IntimateHooks;
import com.l7cor1s.viktoria.quest.QuestDefinition;
import com.l7cor1s.viktoria.relationship.AffinityTier;
import com.l7cor1s.viktoria.relationship.MoodType;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;

import java.util.EnumSet;
import java.util.List;

public final class ViktoriaCharacter extends CompanionCharacter {
    public static final String ID = "viktoria";

    public ViktoriaCharacter() {
        super(ID, Component.translatable("entity.viktoria.companion.viktoria"));
    }

    @Override
    public List<MoodType> supportedMoods() {
        return List.copyOf(EnumSet.allOf(MoodType.class));
    }

    @Override
    public AffinityTier[] affinityTiers() {
        return AffinityTier.values();
    }

    @Override
    public GiftProfile giftProfile() {
        return GiftProfile.builder()
                .loved(Items.POPPY, Items.ROSE_BUSH, Items.PINK_PETALS, Items.CAKE, Items.GOLDEN_APPLE)
                .liked(Items.COOKIE, Items.SWEET_BERRIES, Items.HONEY_BOTTLE, Items.BOOK)
                .likedTag(ItemTags.FLOWERS)
                .hated(Items.ROTTEN_FLESH, Items.POISONOUS_POTATO, Items.SPIDER_EYE)
                .build();
    }

    @Override
    public List<QuestDefinition> quests() {
        return List.of(
                new QuestDefinition("find_flowers", "viktoria.quest.find_flowers", 0, 15),
                new QuestDefinition("share_home", "viktoria.quest.share_home", 25, 20),
                new QuestDefinition("evening_walk", "viktoria.quest.evening_walk", 50, 25)
        );
    }

    @Override
    public IntimateHooks intimateHooks() {
        return IntimateHooks.forCharacter(ID, AffinityTier.CLOSE.minAffinity());
    }
}
