package com.l7cor1s.viktoria.entity;

import com.l7cor1s.viktoria.autonomy.CompanionAutonomy;
import com.l7cor1s.viktoria.character.CharacterRegistry;
import com.l7cor1s.viktoria.character.CompanionCharacter;
import com.l7cor1s.viktoria.character.ViktoriaCharacter;
import com.l7cor1s.viktoria.dialogue.DialogueManager;
import com.l7cor1s.viktoria.gift.GiftHandler;
import com.l7cor1s.viktoria.relationship.CompanionPlayerData;
import com.l7cor1s.viktoria.relationship.CompanionState;
import com.l7cor1s.viktoria.relationship.ModAttachments;
import com.l7cor1s.viktoria.relationship.MoodType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class CompanionEntity extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<String> CHARACTER_ID =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> FOLLOWING =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> ANIM_STATE =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.STRING);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int talkTicks;
    private int stillTicks;
    private int giftTicks;

    public CompanionEntity(EntityType<? extends CompanionEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ARMOR, 2.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHARACTER_ID, ViktoriaCharacter.ID);
        builder.define(FOLLOWING, false);
        builder.define(ANIM_STATE, "idle");
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new CompanionFollowGoal(this, 1.15));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.7) {
            @Override
            public boolean canUse() {
                String anim = getAnimState();
                return !"sit".equals(anim)
                        && !"talk".equals(anim)
                        && !"sleep".equals(anim)
                        && !"look_at_sunset".equals(anim)
                        && super.canUse();
            }
        });
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    public String getCharacterId() {
        return this.entityData.get(CHARACTER_ID);
    }

    public void setCharacterId(String id) {
        this.entityData.set(CHARACTER_ID, id);
    }

    public CompanionCharacter character() {
        return CharacterRegistry.get(getCharacterId()).orElseGet(() -> CharacterRegistry.require(ViktoriaCharacter.ID));
    }

    public boolean isFollowing() {
        return this.entityData.get(FOLLOWING);
    }

    public void setFollowing(boolean following) {
        this.entityData.set(FOLLOWING, following);
    }

    public void setAnimState(String state) {
        this.entityData.set(ANIM_STATE, state);
        if ("talk".equals(state)) {
            this.talkTicks = 200;
        }
        if ("accept_gift".equals(state) || "gift".equals(state)) {
            this.giftTicks = 40;
        }
    }

    public String getAnimState() {
        return this.entityData.get(ANIM_STATE);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            if (talkTicks > 0) {
                talkTicks--;
                if (talkTicks == 0 && !isPoseLocked(getAnimState())) {
                    setAnimState(isFollowing() ? "follow" : "idle");
                }
            }
            if (giftTicks > 0) {
                giftTicks--;
                if (giftTicks == 0 && ("accept_gift".equals(getAnimState()) || "gift".equals(getAnimState()))) {
                    setAnimState(isFollowing() ? "follow" : "idle");
                }
            }
            syncFollowFromOwner();
            updateRestPose();
            CompanionAutonomy.tick(this);
        }
    }

    private static boolean isPoseLocked(String anim) {
        return "accept_gift".equals(anim)
                || "gift".equals(anim)
                || "intimate".equals(anim)
                || "intimate_close".equals(anim)
                || "intimate_embrace".equals(anim)
                || "talk".equals(anim)
                || "sleep".equals(anim)
                || "look_at_sunset".equals(anim);
    }

    private void updateRestPose() {
        String anim = getAnimState();
        if (isPoseLocked(anim) || isFollowing()) {
            stillTicks = 0;
            if ("sit".equals(anim) && isFollowing()) {
                setAnimState("follow");
            }
            return;
        }
        if (getDeltaMovement().horizontalDistanceSqr() > 1.0E-4 || getNavigation().isInProgress()) {
            stillTicks = 0;
            if ("sit".equals(anim)) {
                setAnimState("idle");
            }
            return;
        }
        stillTicks++;
        if (stillTicks > 160 && !"sit".equals(anim) && !"sleep".equals(anim)) {
            setAnimState("sit");
        }
    }

    private void syncFollowFromOwner() {
        Player nearest = level().getNearestPlayer(this, 48.0);
        if (nearest == null) {
            return;
        }
        CompanionState state = nearest.getData(ModAttachments.COMPANION_DATA.get()).getOrCreate(getCharacterId());
        if (state.following() != isFollowing()) {
            setFollowing(state.following());
        }
        long now = level().getGameTime();
        if (state.lastInteractGameTime() > 0 && now - state.lastInteractGameTime() > 24000L) {
            if (state.mood() != MoodType.LONELY && state.mood() != MoodType.IRRITATION) {
                state.setMood(MoodType.LONELY);
            }
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        CompanionPlayerData data = serverPlayer.getData(ModAttachments.COMPANION_DATA.get());
        CompanionState state = data.getOrCreate(getCharacterId());
        state.touch(level().getGameTime());

        // Gift: item in main hand + RMB
        if (!held.isEmpty() && hand == InteractionHand.MAIN_HAND) {
            return GiftHandler.tryGiveGift(serverPlayer, this, held);
        }

        setAnimState("talk");
        DialogueManager.open(serverPlayer, this, "greeting");
        return InteractionResult.CONSUME;
    }

    @Override
    public Component getName() {
        return character().displayName();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("CharacterId", getCharacterId());
        tag.putBoolean("Following", isFollowing());
        tag.putString("AnimState", getAnimState());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("CharacterId")) {
            setCharacterId(tag.getString("CharacterId"));
        }
        setFollowing(tag.getBoolean("Following"));
        if (tag.contains("AnimState")) {
            setAnimState(tag.getString("AnimState"));
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Controllers reserved: idle (breathing), walk, sit, sleep, accept_gift, look_at_sunset (+ talk/follow/intimate)
        controllers.add(new AnimationController<>(this, "main", 8, this::predicate));
    }

    private PlayState predicate(AnimationState<CompanionEntity> state) {
        String anim = getAnimState();
        if ("talk".equals(anim)) {
            state.setAnimation(RawAnimation.begin().thenLoop("talk"));
            return PlayState.CONTINUE;
        }
        if ("accept_gift".equals(anim) || "gift".equals(anim)) {
            state.setAnimation(RawAnimation.begin().thenPlay("accept_gift"));
            return PlayState.CONTINUE;
        }
        if ("intimate".equals(anim)) {
            state.setAnimation(RawAnimation.begin().thenLoop("intimate"));
            return PlayState.CONTINUE;
        }
        if ("intimate_close".equals(anim)) {
            state.setAnimation(RawAnimation.begin().thenLoop("intimate_close"));
            return PlayState.CONTINUE;
        }
        if ("intimate_embrace".equals(anim)) {
            state.setAnimation(RawAnimation.begin().thenLoop("intimate_embrace"));
            return PlayState.CONTINUE;
        }
        if ("sleep".equals(anim)) {
            state.setAnimation(RawAnimation.begin().thenLoop("sleep"));
            return PlayState.CONTINUE;
        }
        if ("look_at_sunset".equals(anim)) {
            state.setAnimation(RawAnimation.begin().thenLoop("look_at_sunset"));
            return PlayState.CONTINUE;
        }
        if (state.isMoving()) {
            state.setAnimation(RawAnimation.begin().thenLoop("walk"));
            return PlayState.CONTINUE;
        }
        if ("sit".equals(anim)) {
            state.setAnimation(RawAnimation.begin().thenLoop("sit"));
            return PlayState.CONTINUE;
        }
        if (isFollowing()) {
            state.setAnimation(RawAnimation.begin().thenLoop("follow"));
            return PlayState.CONTINUE;
        }
        state.setAnimation(RawAnimation.begin().thenLoop("idle"));
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
