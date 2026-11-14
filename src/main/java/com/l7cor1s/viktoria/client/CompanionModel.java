package com.l7cor1s.viktoria.client;

import com.l7cor1s.viktoria.ViktoriaMod;
import com.l7cor1s.viktoria.entity.CompanionEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * Classic player-like geo (base body + outer clothing). Texture is HD 256 only.
 */
public class CompanionModel extends DefaultedEntityGeoModel<CompanionEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ViktoriaMod.MODID, "skins/viktoria-hd-256.png");

    public CompanionModel() {
        super(ResourceLocation.fromNamespaceAndPath(ViktoriaMod.MODID, "viktoria"), true);
    }

    @Override
    public ResourceLocation getTextureResource(CompanionEntity animatable) {
        return TEXTURE;
    }
}
