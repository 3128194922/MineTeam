package com.xiaohunao.mine_team.common.event.subscriber;

import com.xiaohunao.mine_team.MineTeam;
import com.xiaohunao.mine_team.common.team.TeamSync;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MineTeam.MOD_ID)
public class LevelEventSubscriber {
    @SubscribeEvent
    public static void onCreateSpawnPosition(LevelEvent.CreateSpawnPosition event) {
        ServerLevel level = (ServerLevel)event.getLevel();
        TeamSync.ensureTeams(level.getServer().getScoreboard());
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        TeamSync.ensureTeams(event.getServer().getScoreboard());
    }
}
