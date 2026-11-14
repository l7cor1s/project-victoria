package com.l7cor1s.viktoria.spawn;

import com.l7cor1s.viktoria.ViktoriaMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModWorldgen {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, ViktoriaMod.MODID);

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> VIKTORIA_HOUSE =
            FEATURES.register("viktoria_house", () -> new ViktoriaHouseFeature(NoneFeatureConfiguration.CODEC));

    private ModWorldgen() {
    }
}
