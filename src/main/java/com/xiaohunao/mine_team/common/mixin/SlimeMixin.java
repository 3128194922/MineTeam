package com.xiaohunao.mine_team.common.mixin;

import com.xiaohunao.mine_team.common.team.TeamSync;
import com.xiaohunao.mine_team.common.util.TeamUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.PlayerTeam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Slime.class)
public class SlimeMixin {
    @Inject(method = "remove", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/Slime;setCustomName(Lnet/minecraft/network/chat/Component;)V"),
            locals = LocalCapture.CAPTURE_FAILHARD)
    public void remove(Entity.RemovalReason pReason, CallbackInfo ci, int i, Component component, boolean flag, float f, int j, int k, int l, float f1, float f2, Slime slime) {
        Slime thisSlime = (Slime) (Object) this;
        Level level = thisSlime.level();
        if (!level.isClientSide){
            PlayerTeam playersTeam = TeamUtil.getTeam(thisSlime);
            if (playersTeam != null){
                TeamSync.applyTeam(slime, playersTeam.getName(), true);
                slime.setGlowingTag(true);
            }
        }
    }
}
