package com.xiaohunao.mine_team.common.event.subscriber;

import com.xiaohunao.mine_team.MineTeam;
import com.xiaohunao.mine_team.common.capability.TeamCapability;
import com.xiaohunao.mine_team.common.team.TeamSync;
import com.xiaohunao.mine_team.common.util.TeamUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(modid = MineTeam.MOD_ID)
public class MobTeamEventSubscriber {
    @SubscribeEvent
    public static void onPlayerInteractEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Level level = event.getLevel();
        InteractionHand hand = event.getHand();
        Player player = event.getEntity();
        if (hand != InteractionHand.MAIN_HAND) {
            return;
        }
        Entity target = event.getTarget();
        if (target instanceof LivingEntity livingEntity){
            TeamCapability.get(player).ifPresent(teamCapability -> {
                teamCapability.withMobTeam(player,livingEntity,hand);
            });
            if (level instanceof ServerLevel serverLevel) {
                MinecraftServer server = serverLevel.getServer();
                server.tell(new TickTask(server.getTickCount() + 1, () -> {
                    if (!livingEntity.isRemoved()) {
                        TeamSync.syncPet(livingEntity, true);
                    }
                }));
            }
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity livingEntity = event.getEntity();
        TeamCapability.get(livingEntity).ifPresent(teamCapability -> {
            if (teamCapability.data.isFrantic()) {
                teamCapability.tickMobTeam(livingEntity);
            }
        });

        if (livingEntity instanceof Mob
                && !livingEntity.level().isClientSide()
                && livingEntity.tickCount % 20 == 0) {
            TeamSync.syncPet(livingEntity, true);
        }
    }

    @SubscribeEvent
    public static void onAnimalTame(AnimalTameEvent event) {
        TeamSync.applyTeam(event.getAnimal(), TeamUtil.getEffectiveTeamColor(event.getTamer()), true);
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Mob living)) {
            return;
        }

        TeamSync.syncFromScoreboard(living);
        if (TeamUtil.isPet(living)) {
            TeamSync.syncPet(living, true);
        }
    }

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (event.getEntity().level().isClientSide()
                || !(event.getEntity() instanceof Mob mob)
                || event.getNewTarget() == null) {
            return;
        }

        LivingEntity target = event.getNewTarget();
        Player owner = TeamUtil.getOwner(mob);
        if (owner != null && (target == owner || TeamUtil.getOwner(target) == owner)) {
            event.setCanceled(true);
            return;
        }

        if (TeamUtil.isSameTeam(mob, target)) {
            event.setCanceled(true);
        }
    }
}
