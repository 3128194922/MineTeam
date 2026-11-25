package com.xiaohunao.mine_team.common.mixin;

import com.xiaohunao.mine_team.common.util.TeamUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(
            method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void mine_team$canAttackSameTeam(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof Mob mob) || mob.level().isClientSide() || target == null) {
            return;
        }
        if (TeamUtil.isNoTeam(TeamUtil.getEffectiveTeamColor(mob))) {
            return;
        }
        if (TeamUtil.isSameTeam(mob, target)) {
            cir.setReturnValue(false);
        }
    }
}
