package com.l7cor1s.viktoria.spawn;

import com.l7cor1s.viktoria.character.ViktoriaCharacter;
import com.l7cor1s.viktoria.entity.CompanionEntity;
import com.l7cor1s.viktoria.entity.ModEntities;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Compact dark-oak cottage for Viktoria. Spawns her at the doorway when generated.
 */
public class ViktoriaHouseFeature extends Feature<NoneFeatureConfiguration> {
    public ViktoriaHouseFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        if (!level.getBlockState(origin.below()).isSolid()) {
            return false;
        }
        // Avoid oceans / steep cliffs roughly
        if (!level.getFluidState(origin).isEmpty()) {
            return false;
        }

        int size = 5;
        BlockState floor = Blocks.DARK_OAK_PLANKS.defaultBlockState();
        BlockState wall = Blocks.DARK_OAK_LOG.defaultBlockState();
        BlockState roof = Blocks.BLACKSTONE.defaultBlockState();
        BlockState glass = Blocks.WHITE_STAINED_GLASS_PANE.defaultBlockState();

        for (int x = 0; x <= size; x++) {
            for (int z = 0; z <= size; z++) {
                BlockPos floorPos = origin.offset(x, -1, z);
                level.setBlock(floorPos, floor, 3);
                for (int y = 0; y <= 3; y++) {
                    boolean edge = x == 0 || z == 0 || x == size || z == size;
                    BlockPos pos = origin.offset(x, y, z);
                    if (edge && y < 3) {
                        if ((x == size / 2 && z == 0 && y <= 1)) {
                            continue; // doorway
                        }
                        if ((y == 1) && ((x == size / 2 && z == size) || (z == size / 2 && (x == 0 || x == size)))) {
                            level.setBlock(pos, glass, 3);
                        } else {
                            level.setBlock(pos, wall, 3);
                        }
                    } else if (!edge && y == 0) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    } else if (!edge && y > 0 && y < 3) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                    if (y == 3) {
                        level.setBlock(pos, roof, 3);
                    }
                }
            }
        }

        // Door
        BlockPos doorLower = origin.offset(size / 2, 0, 0);
        BlockPos doorUpper = doorLower.above();
        BlockState door = Blocks.DARK_OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.SOUTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        level.setBlock(doorLower, door, 3);
        level.setBlock(doorUpper, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 3);

        // Interior accents — soft bed stand-in, chest, carpet (avoids double-block bed issues in WG)
        level.setBlock(origin.offset(1, 0, 3), Blocks.WHITE_CARPET.defaultBlockState(), 3);
        level.setBlock(origin.offset(2, 0, 3), Blocks.WHITE_CARPET.defaultBlockState(), 3);
        level.setBlock(origin.offset(1, 0, 4), Blocks.WHITE_WOOL.defaultBlockState(), 3);
        level.setBlock(origin.offset(2, 0, 4), Blocks.BLACK_WOOL.defaultBlockState(), 3);
        level.setBlock(origin.offset(4, 0, 1), Blocks.CHEST.defaultBlockState(), 3);
        level.setBlock(origin.offset(4, 0, 4), Blocks.LANTERN.defaultBlockState(), 3);
        level.setBlock(origin.offset(1, 0, 1), Blocks.POTTED_POPPY.defaultBlockState(), 3);

        // Front path + marker lantern outside
        level.setBlock(origin.offset(size / 2, -1, -1), Blocks.COBBLESTONE.defaultBlockState(), 3);
        level.setBlock(origin.offset(size / 2, 0, -1), Blocks.AIR.defaultBlockState(), 3);

        if (random.nextFloat() < 0.85f) {
            CompanionEntity entity = ModEntities.COMPANION.get().create(level.getLevel());
            if (entity != null) {
                entity.setCharacterId(ViktoriaCharacter.ID);
                BlockPos doorStep = origin.offset(size / 2, 0, -1);
                entity.moveTo(doorStep.getX() + 0.5, doorStep.getY(), doorStep.getZ() + 0.5, 0, 0);
                entity.setPersistenceRequired();
                level.addFreshEntity(entity);
            }
        }
        return true;
    }
}
