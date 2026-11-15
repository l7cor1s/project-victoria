package com.l7cor1s.viktoria.entity.ai;

import com.l7cor1s.viktoria.config.VikaConfig;
import com.l7cor1s.viktoria.dialogue.LineFormatter;
import com.l7cor1s.viktoria.entity.Emote;
import com.l7cor1s.viktoria.entity.VikaEntity;
import com.l7cor1s.viktoria.entity.VikaSpeech;
import com.l7cor1s.viktoria.world.VikaWorldChecks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Dances next to a jukebox that plays music.
 *
 * @author vyrriox
 */
public class VikaDanceGoal extends Goal {
    private final VikaEntity laura;
    private BlockPos jukebox;
    private int cooldown;
    private int ticks;

    public VikaDanceGoal(VikaEntity laura) {
        this.laura = laura;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (--cooldown > 0) {
            return false;
        }
        cooldown = 60;
        if (!VikaConfig.danceToJukebox.get() || laura.isFetching() || laura.isAsleep() || laura.brain().isSulking() || laura.isGagged() && laura.getRandom().nextBoolean()) {
            return false;
        }
        jukebox = VikaWorldChecks.findPlayingJukebox(laura, 12);
        return jukebox != null;
    }

    @Override
    public boolean canContinueToUse() {
        return jukebox != null && ticks % 40 != 39 || VikaWorldChecks.findPlayingJukebox(laura, 12) != null;
    }

    @Override
    public void start() {
        ticks = 0;
        if (laura.getRandom().nextInt(3) == 0) {
            VikaSpeech.sayToOwner(laura, "dance.jukebox", LineFormatter.values());
            com.l7cor1s.viktoria.world.VikaAdvancements.award(VikaSpeech.owner(laura), "dance_jukebox");
        }
    }

    @Override
    public void tick() {
        ticks++;
        if (jukebox == null) {
            return;
        }
        if (laura.blockPosition().distSqr(jukebox) > 16) {
            if (ticks % 20 == 1) {
                laura.getNavigation().moveTo(jukebox.getX() + 0.5, jukebox.getY(), jukebox.getZ() + 0.5, 1.0);
            }
            return;
        }
        laura.getNavigation().stop();
        if (!laura.isEmoting()) {
            laura.playEmote(Emote.DANCE);
            laura.brain().onDance();
        }
    }

    @Override
    public void stop() {
        jukebox = null;
        if (laura.getEmote() == Emote.DANCE) {
            laura.playEmote(Emote.NONE);
        }
    }
}
