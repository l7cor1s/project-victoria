package com.l7cor1s.viktoria.client;

import com.l7cor1s.viktoria.ViktoriaMod;
import com.l7cor1s.viktoria.entity.CompanionEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class CompanionModel extends DefaultedEntityGeoModel<CompanionEntity> {
    public CompanionModel() {
        super(ResourceLocation.fromNamespaceAndPath(ViktoriaMod.MODID, "viktoria"), true);
    }
}
