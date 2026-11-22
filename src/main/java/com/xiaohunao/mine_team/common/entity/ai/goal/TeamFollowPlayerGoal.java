package com.xiaohunao.mine_team.common.entity.ai.goal;

import com.xiaohunao.mine_team.common.util.TeamUtil;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class TeamFollowPlayerGoal extends Goal {
    private static final double START_DISTANCE_SQR = 16.0D * 16.0D;
    private static final double STOP_DISTANCE_SQR = 6.0D * 6.0D;
    private static final double SEARCH_RADIUS_SQR = 32.0D * 32.0D;

    private final Mob mob;
    private final double speedModifier;
    @Nullable
    private Player followTarget;
    private int nextSearchTick;

    public TeamFollowPlayerGoal(Mob mob, double speedModifier) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.mob.level().isClientSide()
                || this.mob.isNoAi()
                || this.mob.isPassenger()
                || this.mob.isVehicle()
                || !this.hasNavigation()
                || TeamUtil.isSitting(this.mob)
                || TeamUtil.isFrantic(this.mob)
                || TeamUtil.isNoTeam(TeamUtil.getEffectiveTeamColor(this.mob))) {
            return false;
        }

        if (this.mob.getTarget() != null && this.mob.getTarget().isAlive()) {
            return false;
        }

        if (this.mob.tickCount < this.nextSearchTick) {
            return this.isValidTarget(this.followTarget)
                    && this.mob.distanceToSqr(this.followTarget) > START_DISTANCE_SQR;
        }

        this.nextSearchTick = this.mob.tickCount + 20;
        this.followTarget = this.findTarget();
        return this.followTarget != null && this.mob.distanceToSqr(this.followTarget) > START_DISTANCE_SQR;
    }

    @Override
    public boolean canContinueToUse() {
        if (!this.hasNavigation() || !this.isValidTarget(this.followTarget)) {
            return false;
        }
        if (this.mob.getTarget() != null && this.mob.getTarget().isAlive()) {
            return false;
        }
        if (TeamUtil.isSitting(this.mob)) {
            return false;
        }
        return this.mob.distanceToSqr(this.followTarget) > STOP_DISTANCE_SQR;
    }

    @Override
    public void start() {
        if (this.followTarget != null && this.hasNavigation()) {
            this.mob.getNavigation().moveTo(this.followTarget, this.speedModifier);
        }
    }

    @Override
    public void tick() {
        if (this.followTarget == null || !this.hasNavigation()) {
            return;
        }

        if (this.mob.distanceToSqr(this.followTarget) <= STOP_DISTANCE_SQR) {
            this.mob.getNavigation().stop();
            this.mob.getLookControl().setLookAt(this.followTarget, 10.0F, (float) this.mob.getMaxHeadXRot());
            return;
        }

        if (this.mob.tickCount % 10 == 0 || this.mob.getNavigation().isDone()) {
            this.mob.getNavigation().moveTo(this.followTarget, this.speedModifier);
        }
    }

    @Override
    public void stop() {
        this.followTarget = null;
        if (this.hasNavigation()) {
            this.mob.getNavigation().stop();
        }
    }

    private boolean hasNavigation() {
        return this.mob.getNavigation() != null;
    }

    private boolean isValidTarget(@Nullable Player player) {
        if (player == null || !player.isAlive() || player.isSpectator() || player.isRemoved()) {
            return false;
        }
        if (player.level() != this.mob.level()) {
            return false;
        }
        return TeamUtil.isSameTeam(this.mob, player);
    }

    @Nullable
    private Player findTarget() {
        Player owner = TeamUtil.getOwner(this.mob);
        if (this.isValidTarget(owner)) {
            return owner;
        }

        Player best = null;
        double bestDistance = SEARCH_RADIUS_SQR;
        for (Player player : this.mob.level().players()) {
            if (!this.isValidTarget(player)) {
                continue;
            }
            double distance = this.mob.distanceToSqr(player);
            if (distance <= bestDistance) {
                bestDistance = distance;
                best = player;
            }
        }
        return best;
    }
}
