package com.l7cor1s.viktoria.neoforge;

import com.l7cor1s.viktoria.ViktoriaMod;
import com.l7cor1s.viktoria.block.VikaGraveBlock;
import com.l7cor1s.viktoria.entity.VikaEntity;
import com.l7cor1s.viktoria.event.VikaEvents;
import com.l7cor1s.viktoria.inventory.VikaInventoryMenu;
import com.l7cor1s.viktoria.item.VikaHeartItem;
import com.l7cor1s.viktoria.network.VikaNetwork;
import com.l7cor1s.viktoria.registry.VikaRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * NeoForge entry point: registers the content, forwards the game events to the common code and
 * carries the mod packets.
 *
 * @author vyrriox
 */
@Mod(ViktoriaMod.MODID)
public final class VikaNeoForge {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, ViktoriaMod.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ViktoriaMod.MODID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ViktoriaMod.MODID);
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, ViktoriaMod.MODID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ViktoriaMod.MODID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ViktoriaMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<VikaEntity>> LAURA = ENTITY_TYPES.register(VikaRegistries.LAURA_ID,
            () -> VikaRegistries.lauraType().build(ViktoriaMod.id(VikaRegistries.LAURA_ID).toString()));
    public static final DeferredBlock<VikaGraveBlock> GRAVE = BLOCKS.register(VikaRegistries.GRAVE_ID,
            () -> new VikaGraveBlock(VikaRegistries.graveProperties()));
    public static final DeferredItem<BlockItem> GRAVE_ITEM = ITEMS.registerSimpleBlockItem(VikaRegistries.GRAVE_ID, GRAVE);
    public static final DeferredItem<VikaHeartItem> HEART = ITEMS.register(VikaRegistries.HEART_ID,
            () -> new VikaHeartItem(VikaRegistries.heartProperties()));
    public static final DeferredItem<DeferredSpawnEggItem> SPAWN_EGG = ITEMS.register(VikaRegistries.SPAWN_EGG_ID,
            () -> new DeferredSpawnEggItem(LAURA, VikaRegistries.EGG_PRIMARY, VikaRegistries.EGG_SECONDARY, new Item.Properties()));
    public static final DeferredHolder<MenuType<?>, MenuType<VikaInventoryMenu>> INVENTORY_MENU = MENUS.register(VikaRegistries.INVENTORY_MENU_ID,
            VikaRegistries::inventoryMenuType);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register(VikaRegistries.TAB_ID, () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.viktoria"))
            .icon(() -> new ItemStack(HEART.get()))
            .displayItems((parameters, output) -> {
                output.accept(HEART.get());
                output.accept(SPAWN_EGG.get());
                output.accept(GRAVE_ITEM.get());
            })
            .build());

    static {
        VikaRegistries.LAURA = LAURA;
        VikaRegistries.GRAVE = GRAVE::get;
        VikaRegistries.GRAVE_ITEM = GRAVE_ITEM::get;
        VikaRegistries.HEART = HEART::get;
        VikaRegistries.SPAWN_EGG = SPAWN_EGG::get;
        VikaRegistries.INVENTORY_MENU = INVENTORY_MENU;
        for (VikaRegistries.Sound sound : VikaRegistries.Sound.values()) {
            VikaRegistries.SOUNDS.put(sound, SOUNDS.register(sound.id(), () -> SoundEvent.createVariableRangeEvent(VikaRegistries.soundId(sound))));
        }
    }

    public VikaNeoForge(IEventBus modBus, ModContainer container, Dist dist) {
        ViktoriaMod.init(new NeoForgePlatform(container));
        ENTITY_TYPES.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        SOUNDS.register(modBus);
        MENUS.register(modBus);
        TABS.register(modBus);

        modBus.addListener(EntityAttributeCreationEvent.class, event -> event.put(LAURA.get(), VikaEntity.createAttributes().build()));
        modBus.addListener(RegisterPayloadHandlersEvent.class, VikaNeoForge::registerPayloads);
        modBus.addListener(BuildCreativeModeTabContentsEvent.class, event -> {
            if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
                event.accept(SPAWN_EGG.get());
            }
        });

        IEventBus game = NeoForge.EVENT_BUS;
        game.addListener(ServerStartingEvent.class, event -> VikaEvents.onServerStarting(event.getServer()));
        game.addListener(ServerStartedEvent.class, event -> VikaEvents.onServerStarted(event.getServer()));
        game.addListener(ServerStoppedEvent.class, event -> VikaEvents.onServerStopped(event.getServer()));
        game.addListener(ServerTickEvent.Post.class, event -> VikaEvents.onServerTick(event.getServer()));
        game.addListener(RegisterCommandsEvent.class, event -> VikaEvents.registerCommands(event.getDispatcher()));
        game.addListener(ServerChatEvent.class, event -> VikaEvents.onChat(event.getPlayer(), event.getRawText()));
        game.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                VikaEvents.onPlayerJoin(player);
            }
        });
        game.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                VikaEvents.onPlayerLeave(player);
            }
        });
        game.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                ServerLevel from = player.server.getLevel(event.getFrom());
                VikaEvents.onPlayerChangedDimension(player, from);
            }
        });
        game.addListener(EntityLeaveLevelEvent.class, event -> {
            if (!event.getLevel().isClientSide()) {
                VikaEvents.onEntityUnloaded(event.getEntity());
            }
        });
        game.addListener(LivingDeathEvent.class, event -> {
            if (event.getEntity().level().isClientSide()) {
                return;
            }
            if (event.getEntity() instanceof ServerPlayer player) {
                VikaEvents.onPlayerDeath(player);
            } else if (event.getSource().getEntity() instanceof ServerPlayer killer) {
                VikaEvents.onEntityKilled(event.getEntity(), killer);
            }
        });
        game.addListener(BlockEvent.BreakEvent.class, event -> {
            if (event.getPlayer() instanceof ServerPlayer player) {
                VikaEvents.onBlockBroken(player, event.getState());
            }
        });

        if (dist.isClient()) {
            VikaNeoForgeClient.init(modBus);
        }
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(String.valueOf(VikaNetwork.PROTOCOL));
        registrar.playBidirectional(VikaPayload.TYPE, VikaPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                context.enqueueWork(() -> VikaNetwork.handleServer(player, payload.data()));
            } else {
                context.enqueueWork(() -> ViktoriaMod.client().handlePacket(payload.data()));
            }
        });
    }
}
