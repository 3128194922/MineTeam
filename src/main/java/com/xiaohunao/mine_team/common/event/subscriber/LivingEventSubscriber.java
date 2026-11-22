package com.xiaohunao.mine_team.common.event.subscriber;

import com.xiaohunao.mine_team.MineTeam;
import com.xiaohunao.mine_team.common.capability.TeamCapability;
import com.xiaohunao.mine_team.common.team.TeamSync;
import com.xiaohunao.mine_team.common.util.TeamUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(modid = MineTeam.MOD_ID)
public class LivingEventSubscriber {
    @SubscribeEvent
    public static void onPlayerLoggedInEvent(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TeamSync.syncPlayer(player, true);
        }
    }

    @SubscribeEvent
    public static void onLivingPvP(LivingAttackEvent event) {
        DamageSource source = event.getSource();
        Entity sourceEntity = source.getEntity();
        if (!(sourceEntity instanceof LivingEntity attackEntity)) {
            return;
        }

        TeamCapability capability = TeamCapability.get(attackEntity).orElse(null);
        if (capability == null) {
            return;
        }

        if (!capability.canAttack(attackEntity, event.getEntity())) {
            event.setCanceled(true);
            return;
        }
        capability.setLastHurtMob(attackEntity, event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerCloneEvent(PlayerEvent.Clone event) {
        Player player = event.getEntity();
        Player original = event.getOriginal();
        if (player.level().isClientSide()) {
            return;
        }

        TeamCapability.get(player).ifPresent(teamCapability -> {
            TeamCapability originalCapability = null;
            original.reviveCaps();
            try {
                originalCapability = TeamCapability.get(original).orElse(null);
                if (originalCapability != null) {
                    teamCapability.data.copy(originalCapability.data);
                }
            } finally {
                original.invalidateCaps();
            }

            if (originalCapability == null && player.level() instanceof ServerLevel serverLevel) {
                PlayerTeam scoreboardTeam = serverLevel.getServer().getScoreboard()
                        .getPlayersTeam(original.getScoreboardName());
                if (scoreboardTeam != null && TeamUtil.isValidColor(scoreboardTeam.getName())) {
                    teamCapability.data.setColor(TeamUtil.normalizeColor(scoreboardTeam.getName()));
                }
            }
            teamCapability.data.setColor(TeamUtil.normalizeColor(teamCapability.data.getColor()));
        });
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        TeamSync.syncPlayer(player, true);
        scheduleDelayedSync(player);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        TeamSync.syncPlayer(player, false);
        scheduleDelayedSync(player);
    }

    private static void scheduleDelayedSync(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        server.tell(new TickTask(server.getTickCount() + 2, () -> {
            if (!player.isRemoved()) {
                TeamSync.send(player);
            }
        }));
    }
}
