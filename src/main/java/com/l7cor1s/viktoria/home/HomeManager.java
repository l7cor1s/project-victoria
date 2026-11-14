package com.l7cor1s.viktoria.home;

import com.l7cor1s.viktoria.entity.CompanionEntity;
import com.l7cor1s.viktoria.quest.QuestProgress;
import com.l7cor1s.viktoria.relationship.CompanionState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Shared home / base hooks — claim bed area and live-together flag. */
public final class HomeManager {
    private HomeManager() {
    }

    public static void claimHome(ServerPlayer player, CompanionEntity companion, CompanionState state) {
        BlockPos pos = player.blockPosition();
        state.setHomePos(pos);
        player.displayClientMessage(Component.translatable(companion.character().homeClaimLangKey(),
                pos.getX(), pos.getY(), pos.getZ()), false);
        QuestProgress.onHomeClaimed(player, companion, state);
    }

    public static void setLivesTogether(ServerPlayer player, CompanionEntity companion, CompanionState state, boolean value) {
        if (state.homePos() == null) {
            claimHome(player, companion, state);
        }
        if (state.affinity() < 25) {
            player.displayClientMessage(Component.translatable("viktoria.home.need_affinity"), true);
            return;
        }
        state.setLivesTogether(value);
        player.displayClientMessage(Component.translatable(
                value ? "viktoria.home.live_on" : "viktoria.home.live_off",
                companion.character().displayName()), false);
    }
}
