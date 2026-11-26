package com.xiaohunao.mine_team.common.entity.ai.goal;

import com.xiaohunao.mine_team.common.util.TeamUtil;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

public class TeamRivalTargetGoal extends NearestAttackableTargetGoal<Mob> {
    public TeamRivalTargetGoal(Mob mob) {
        super(mob, Mob.class, 10, true, false,
                target -> TeamUtil.isRivalTeam(mob, target) && TeamUtil.isHostileForTeamFight(target));
    }

    @Override
    public boolean canUse() {
        if (TeamUtil.isNoTeam(TeamUtil.getEffectiveTeamColor(this.mob))
                || TeamUtil.isTamedPet(this.mob)
                || !TeamUtil.isHostileForTeamFight(this.mob)) {
            return false;
        }
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        if (TeamUtil.isNoTeam(TeamUtil.getEffectiveTeamColor(this.mob))
                || TeamUtil.isTamedPet(this.mob)
                || !TeamUtil.isHostileForTeamFight(this.mob)) {
            return false;
        }
        return super.canContinueToUse();
    }
}
