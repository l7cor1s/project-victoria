package com.l7cor1s.viktoria.client;

import com.l7cor1s.viktoria.config.ViktoriaConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Cloth Config UI bound to {@link ViktoriaConfig} ModConfigSpec values. */
public final class ViktoriaConfigScreen {
    private ViktoriaConfigScreen() {
    }

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("viktoria.config.title"))
                .setSavingRunnable(() -> {
                    // ModConfigSpec values are written by Cloth setters via setSaveConsumer below.
                });

        ConfigEntryBuilder entry = builder.entryBuilder();
        ConfigCategory behavior = builder.getOrCreateCategory(Component.translatable("viktoria.config.behavior"));
        behavior.addEntry(entry.startBooleanToggle(Component.translatable("viktoria.config.autonomy"),
                        ViktoriaConfig.AUTONOMY_ENABLED.get())
                .setDefaultValue(true)
                .setSaveConsumer(ViktoriaConfig.AUTONOMY_ENABLED::set)
                .build());
        behavior.addEntry(entry.startBooleanToggle(Component.translatable("viktoria.config.distant_chat"),
                        ViktoriaConfig.DISTANT_CHAT_ENABLED.get())
                .setDefaultValue(true)
                .setSaveConsumer(ViktoriaConfig.DISTANT_CHAT_ENABLED::set)
                .build());
        behavior.addEntry(entry.startBooleanToggle(Component.translatable("viktoria.config.weather"),
                        ViktoriaConfig.WEATHER_REACTIONS.get())
                .setDefaultValue(true)
                .setSaveConsumer(ViktoriaConfig.WEATHER_REACTIONS::set)
                .build());
        behavior.addEntry(entry.startBooleanToggle(Component.translatable("viktoria.config.biome"),
                        ViktoriaConfig.BIOME_REACTIONS.get())
                .setDefaultValue(true)
                .setSaveConsumer(ViktoriaConfig.BIOME_REACTIONS::set)
                .build());

        ConfigCategory spawn = builder.getOrCreateCategory(Component.translatable("viktoria.config.spawn"));
        spawn.addEntry(entry.startDoubleField(Component.translatable("viktoria.config.auto_spawn"),
                        ViktoriaConfig.AUTO_SPAWN_CHANCE.get())
                .setDefaultValue(0.65d)
                .setMin(0.0d)
                .setMax(1.0d)
                .setSaveConsumer(ViktoriaConfig.AUTO_SPAWN_CHANCE::set)
                .build());

        ConfigCategory affinity = builder.getOrCreateCategory(Component.translatable("viktoria.config.affinity"));
        affinity.addEntry(entry.startIntField(Component.translatable("viktoria.config.friend_threshold"),
                        ViktoriaConfig.AFFINITY_FRIEND_THRESHOLD.get())
                .setDefaultValue(35)
                .setMin(1)
                .setMax(99)
                .setSaveConsumer(ViktoriaConfig.AFFINITY_FRIEND_THRESHOLD::set)
                .build());
        affinity.addEntry(entry.startIntField(Component.translatable("viktoria.config.close_threshold"),
                        ViktoriaConfig.AFFINITY_CLOSE_THRESHOLD.get())
                .setDefaultValue(60)
                .setMin(1)
                .setMax(99)
                .setSaveConsumer(ViktoriaConfig.AFFINITY_CLOSE_THRESHOLD::set)
                .build());
        affinity.addEntry(entry.startIntField(Component.translatable("viktoria.config.partner_threshold"),
                        ViktoriaConfig.AFFINITY_PARTNER_THRESHOLD.get())
                .setDefaultValue(85)
                .setMin(1)
                .setMax(99)
                .setSaveConsumer(ViktoriaConfig.AFFINITY_PARTNER_THRESHOLD::set)
                .build());

        ConfigCategory intimate = builder.getOrCreateCategory(Component.translatable("viktoria.config.intimate"));
        intimate.addEntry(entry.startBooleanToggle(Component.translatable("viktoria.config.intimate_enabled"),
                        ViktoriaConfig.INTIMATE_SCENES_ENABLED.get())
                .setDefaultValue(true)
                .setSaveConsumer(ViktoriaConfig.INTIMATE_SCENES_ENABLED::set)
                .build());

        return builder.build();
    }
}
