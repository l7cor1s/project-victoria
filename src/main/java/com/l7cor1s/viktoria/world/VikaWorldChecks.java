package com.l7cor1s.viktoria.world;

import com.l7cor1s.viktoria.desire.DesireTable;
import com.l7cor1s.viktoria.entity.VikaEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * World queries used by her needs and desires. Kept in one place because several of them use
 * vanilla APIs that change between Minecraft versions.
 *
 * @author vyrriox
 */
public final class VikaWorldChecks {
    public enum Climate {
        COLD, MILD, HOT
    }

    private static final Map<VikaEntity, long[]> MUSIC_CACHE = new WeakHashMap<>();

    private VikaWorldChecks() {
    }

    public static boolean isNight(Level level) {
        long time = level.getDayTime() % 24000L;
        return time >= 13000L && time < 23000L;
    }

    public static boolean isSunset(Level level) {
        long time = level.getDayTime() % 24000L;
        return time >= 11800L && time < 13300L && !level.isRaining();
    }

    public static boolean canSeeSky(VikaEntity laura) {
        return laura.level().canSeeSky(laura.blockPosition().above());
    }

    /** True when a jukebox plays music within 12 blocks. Cached for 3 seconds. */
    public static boolean isMusicPlaying(VikaEntity laura) {
        long now = laura.level().getGameTime();
        long[] cached = MUSIC_CACHE.get(laura);
        if (cached != null && now - cached[0] < 60) {
            return cached[1] != 0;
        }
        boolean playing = findPlayingJukebox(laura, 12) != null;
        MUSIC_CACHE.put(laura, new long[]{now, playing ? 1 : 0});
        return playing;
    }

    public static BlockPos findPlayingJukebox(VikaEntity laura, int radius) {
        Level level = laura.level();
        BlockPos center = laura.blockPosition();
        ChunkPos min = new ChunkPos(center.offset(-radius, 0, -radius));
        ChunkPos max = new ChunkPos(center.offset(radius, 0, radius));
        for (int cx = min.x; cx <= max.x; cx++) {
            for (int cz = min.z; cz <= max.z; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(cx, cz);
                for (Map.Entry<BlockPos, BlockEntity> e : chunk.getBlockEntities().entrySet()) {
                    if (e.getValue() instanceof JukeboxBlockEntity jukebox && e.getKey().distSqr(center) <= radius * radius) {
                        BlockState state = jukebox.getBlockState();
                        if (state.hasProperty(JukeboxBlock.HAS_RECORD) && state.getValue(JukeboxBlock.HAS_RECORD) && jukebox.getSongPlayer().isPlaying()) {
                            return e.getKey();
                        }
                    }
                }
            }
        }
        return null;
    }

    public static boolean isNearLitCampfire(VikaEntity laura) {
        Level level = laura.level();
        for (BlockPos pos : BlockPos.betweenClosed(laura.blockPosition().offset(-4, -2, -4), laura.blockPosition().offset(4, 2, 4))) {
            if (CampfireBlock.isLitCampfire(level.getBlockState(pos))) {
                return true;
            }
        }
        return false;
    }

    public static boolean ownerHasPetNear(VikaEntity laura, ServerPlayer owner) {
        return !laura.level().getEntitiesOfClass(TamableAnimal.class, laura.getBoundingBox().inflate(8),
                pet -> pet != laura && !(pet instanceof VikaEntity) && pet.isTame() && pet.isOwnedBy(owner)).isEmpty();
    }

    public static Climate climate(VikaEntity laura) {
        Level level = laura.level();
        if (level.dimension() == Level.NETHER) {
            return Climate.HOT;
        }
        Holder<Biome> biome = level.getBiome(laura.blockPosition());
        if (biome.value().coldEnoughToSnow(laura.blockPosition())) {
            return Climate.COLD;
        }
        return biome.value().getBaseTemperature() >= 1.5F ? Climate.HOT : Climate.MILD;
    }

    /** True if she stands in one of the biomes (or dimensions) of the place. */
    public static boolean isInPlace(VikaEntity laura, DesireTable.PlaceDesire place) {
        if (place == null) {
            return false;
        }
        Level level = laura.level();
        Holder<Biome> biome = level.getBiome(laura.blockPosition());
        for (String target : place.targets()) {
            String t = target.trim();
            if (t.startsWith("dimension:")) {
                ResourceLocation dim = ResourceLocation.tryParse(t.substring("dimension:".length()));
                if (dim != null && level.dimension().location().equals(dim)) {
                    return true;
                }
            } else if (t.startsWith("#")) {
                ResourceLocation tag = ResourceLocation.tryParse(t.substring(1));
                if (tag != null && biome.is(TagKey.create(Registries.BIOME, tag))) {
                    return true;
                }
            } else {
                ResourceLocation id = ResourceLocation.tryParse(t);
                if (id != null && biome.is(ResourceKey.create(Registries.BIOME, id))) {
                    return true;
                }
            }
        }
        return false;
    }
}
