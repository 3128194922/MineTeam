package com.xiaohunao.mine_team.common.mixin;

import com.xiaohunao.mine_team.common.mixed.MobMixed;
import com.xiaohunao.mine_team.common.util.TeamUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.PlayerTeam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobMixin extends Entity implements MobMixed {
    @Unique
    private static final EntityDataAccessor<String> DATA_FLAGS_ID = SynchedEntityData.defineId(MobMixin.class, EntityDataSerializers.STRING);

    public MobMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "defineSynchedData", at = @At("RETURN"))
    private void defineSynchedData(CallbackInfo ci) {
        this.entityData.define(DATA_FLAGS_ID, "");
    }


    @Override
    public void setTame(PlayerTeam tame) {
        this.entityData.set(DATA_FLAGS_ID, tame == null ? "" : tame.getName());
    }

    @Override
    public boolean wantsToAttack(LivingEntity ownerLastHurt, PlayerTeam lastHurtTeam,PlayerTeam team) {
        if (ownerLastHurt == null || ownerLastHurt == (LivingEntity) (Object) this) {
            return false;
        }
        if (team == null || TeamUtil.isNoTeam(team.getName())) {
            return false;
        }
        if (lastHurtTeam != null
                && !TeamUtil.isNoTeam(lastHurtTeam.getName())
                && lastHurtTeam.getName().equals(team.getName())) {
            return false;
        }
        return !TeamUtil.isSameTeam((LivingEntity) (Object) this, ownerLastHurt);
    }

    @Override
    public PlayerTeam getOwnerTeam() {
        if (this.getServer() == null || TeamUtil.isFrantic((LivingEntity) (Object) this)) {
            return null;
        }
        return TeamUtil.getTeam((LivingEntity) (Object) this);
    }
}
