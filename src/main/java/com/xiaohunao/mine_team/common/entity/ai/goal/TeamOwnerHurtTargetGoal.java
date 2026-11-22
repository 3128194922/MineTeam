package com.xiaohunao.mine_team.common.entity.ai.goal;

import com.xiaohunao.mine_team.common.mixed.MobMixed;
import com.xiaohunao.mine_team.common.mixed.PlayerTeamMixed;
import com.xiaohunao.mine_team.common.util.TeamUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;

import java.util.EnumSet;

public class TeamOwnerHurtTargetGoal extends TargetGoal {
    private final Mob tameLivingEntity;
    private LivingEntity ownerLastHurt;
    private long timestamp;
    public TeamOwnerHurtTargetGoal(Mob tameLivingEntity) {
        super(tameLivingEntity, false);
        this.tameLivingEntity = tameLivingEntity;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    public boolean canUse() {
        if (!(this.tameLivingEntity instanceof MobMixed mobMixed)) {
            return false;
        }

        PlayerTeam ownerTeam = mobMixed.getOwnerTeam();
        if (ownerTeam == null || TeamUtil.isNoTeam(ownerTeam.getName()) || TeamUtil.isSitting(this.tameLivingEntity)) {
            return false;
        }
        if (!(ownerTeam instanceof PlayerTeamMixed playerTeamMixed)) {
            return false;
        }

        this.ownerLastHurt = playerTeamMixed.mineTeam$getLastHurtMob();
        long timestamp = playerTeamMixed.mineTeam$getLastHurtMobTimestamp();
        if (this.ownerLastHurt == null
                || this.ownerLastHurt == this.tameLivingEntity
                || !this.ownerLastHurt.isAlive()
                || TeamUtil.isSameTeam(this.tameLivingEntity, this.ownerLastHurt)) {
            return false;
        }
        Player owner = TeamUtil.getOwner(this.tameLivingEntity);
        if (owner != null) {
            if (this.ownerLastHurt == owner) {
                return false;
            }
            Player targetOwner = TeamUtil.getOwner(this.ownerLastHurt);
            if (targetOwner != null && targetOwner == owner) {
                return false;
            }
        }
        if (this.ownerLastHurt instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        if (this.tameLivingEntity.distanceToSqr(this.ownerLastHurt) > 32.0D * 32.0D) {
            return false;
        }

        return timestamp != this.timestamp
                && this.canAttack(this.ownerLastHurt, TargetingConditions.DEFAULT)
                && mobMixed.wantsToAttack(this.ownerLastHurt, playerTeamMixed.mineTeam$getLastHurtTeam(), ownerTeam);
    }

    public void start() {
        if (this.ownerLastHurt != null) {
            this.mob.setTarget(this.ownerLastHurt);
        }

        if (this.tameLivingEntity instanceof MobMixed mobMixed){
            PlayerTeam team = mobMixed.getOwnerTeam();
            if (team instanceof PlayerTeamMixed playerTeamMixed) {
                this.timestamp = playerTeamMixed.mineTeam$getLastHurtMobTimestamp();
            }
        }
        super.start();
    }
}
