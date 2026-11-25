package com.xiaohunao.mine_team.common.capability;

import com.xiaohunao.mine_team.common.config.MineTeamConfig;
import com.xiaohunao.mine_team.common.init.ModCapability;
import com.xiaohunao.mine_team.common.mixed.PlayerTeamMixed;
import com.xiaohunao.mine_team.common.network.NetworkHandler;
import com.xiaohunao.mine_team.common.network.s2c.TeamDataSyncS2CPayload;
import com.xiaohunao.mine_team.common.team.TeamSync;
import com.xiaohunao.mine_team.common.util.TeamUtil;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;


public class TeamCapability{
    public TeamData data;
    public TeamCapability(LivingEntity livingEntity) {
        TeamData teamData = new TeamData(livingEntity.getType(), "white", false, new Random().nextInt(2401) + 3600, false, Ingredient.of(Items.GOLDEN_APPLE), true);
        this.data = TeamDataManager.INSTANCE.getOrDefault(livingEntity.getType(), teamData);
    }

    public static LazyOptional<TeamCapability> get(LivingEntity living){
        return living.getCapability(ModCapability.TEAM);
    }

    public boolean canAttack(LivingEntity attackEntity, LivingEntity hurtEntity) {
        if (!(attackEntity.level() instanceof ServerLevel)) {
            return true;
        }

        if (attackEntity == hurtEntity) {
            return MineTeamConfig.allowDamageSelf.get();
        }

        Player attackerOwner = TeamUtil.getOwner(attackEntity);
        Player hurtOwner = TeamUtil.getOwner(hurtEntity);
        if (attackerOwner != null
                && (attackerOwner == hurtEntity || (hurtOwner != null && attackerOwner == hurtOwner))) {
            return false;
        }

        if (!TeamUtil.isSameTeam(attackEntity, hurtEntity)) {
            return true;
        }

        if (attackEntity instanceof Player player && TeamUtil.isPet(hurtEntity)) {
            return player.isShiftKeyDown() && TeamUtil.isOwnedBy(hurtEntity, player.getUUID());
        }

        if (!(attackEntity instanceof Player)) {
            return false;
        }

        boolean attackPvP = this.data.isPvP();
        boolean hurtPvP = get(hurtEntity).map(capability -> capability.data.isPvP()).orElse(false);
        return attackPvP || hurtPvP;
    }

    public void setLastHurtMob(LivingEntity attackEntity, LivingEntity hurtEntity) {
        if (!(hurtEntity.level() instanceof ServerLevel)) {
            return;
        }

        PlayerTeam attackEntityTeam = TeamUtil.getTeam(attackEntity);
        PlayerTeam hurtEntityTeam = TeamUtil.getTeam(hurtEntity);

        if (attackEntity instanceof Player && attackEntityTeam instanceof PlayerTeamMixed playerTeamMixed) {
            playerTeamMixed.mineTeam$setLastHurtMob(hurtEntity);
            playerTeamMixed.mineTeam$setLastHurtTeam(hurtEntityTeam);
        }
        if (hurtEntity instanceof Player && hurtEntityTeam instanceof PlayerTeamMixed playerTeamMixed) {
            playerTeamMixed.mineTeam$setLastHurtMob(attackEntity);
            playerTeamMixed.mineTeam$setLastHurtTeam(attackEntityTeam);
        }
    }

    public void init(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            TeamSync.syncPlayer(serverPlayer, true);
        }
    }

    public void withMobTeam(Player player, LivingEntity interactEntity, InteractionHand hand) {
        ItemStack handStack = player.getItemInHand(hand);
        if (interactEntity instanceof Player){
            return;
        }

        Level level = player.level();
        TeamCapability.get(interactEntity).ifPresent(teamCapability -> {
            TeamData interactEntityTeamData = teamCapability.data;
            if (interactEntityTeamData.isCanTeam() && interactEntityTeamData.getTeamIngredient().test(handStack)){
                PlayerTeam playersTeam = level.getScoreboard().getPlayersTeam(interactEntity.getScoreboardName());
                if (interactEntity.hasEffect(MobEffects.WEAKNESS)
                        && (playersTeam == null || TeamUtil.isNoTeam(playersTeam.getName()))
                        && !TeamUtil.isPet(interactEntity)
                        && !level.isClientSide){
                    interactEntityTeamData.setFranticTime(interactEntityTeamData.getFranticTime());
                    interactEntityTeamData.setColor(TeamUtil.normalizeColor(data.getColor()));
                    interactEntityTeamData.setFrantic(true);
                    interactEntity.getPersistentData().putString("OwnerName", player.getUUID().toString());
                    NetworkHandler.CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension),new TeamDataSyncS2CPayload(interactEntity.getId(),interactEntityTeamData));

                    handStack.hurtAndBreak(1, player, (playerEntity) -> playerEntity.broadcastBreakEvent(hand));

                    interactEntity.removeEffect(MobEffects.WEAKNESS);
                    interactEntity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Math.min(level.getDifficulty().getId() - 1, 0)));
                }
            }
        });


    }

    public void tickMobTeam(LivingEntity livingEntity) {
        Level level = livingEntity.level();
        if (data.getFranticTime() >= 0){
            if (!level.isClientSide){
                data.setFranticTime(data.getFranticTime() - 1);
                ServerLevel serverLevel = (ServerLevel) level;
                NetworkHandler.CHANNEL.send(PacketDistributor.DIMENSION.with(serverLevel::dimension),new TeamDataSyncS2CPayload(livingEntity.getId(), data));
            }else {
                for (int i = 0; i < 2; i++) {
                    double offsetX = level.random.nextGaussian() * 0.02D;
                    double offsetY = level.random.nextGaussian() * 0.02D;
                    double offsetZ = level.random.nextGaussian() * 0.02D;
                    double x = livingEntity.getX() + (double) (level.random.nextFloat() * livingEntity.getBbWidth() * 2.0F) - (double) livingEntity.getBbWidth();
                    double y = livingEntity.getY() + 0.5D + (double) (level.random.nextFloat() * livingEntity.getBbHeight());
                    double z = livingEntity.getZ() + (double) (level.random.nextFloat() * livingEntity.getBbWidth() * 2.0F) - (double) livingEntity.getBbWidth();
                    level.addParticle((ParticleOptions) ParticleTypes.EFFECT, x, y, z, offsetX, offsetY, offsetZ);
                }
            }
        }

        if (data.getFranticTime() == -1){
            if (!level.isClientSide){
                TeamSync.applyTeam(livingEntity, data.getColor(), false);
                livingEntity.setGlowingTag(true);
                data.setFrantic(false);
                data.setFranticTime(data.getTotalFranticTime());
                TeamSync.send(livingEntity);
            }
        }
    }

    public static class Provider implements ICapabilitySerializable<Tag> {
        private final LazyOptional<TeamCapability> instance;
        private final LivingEntity livingEntity;

        public Provider(LivingEntity livingEntity) {
            this.livingEntity = livingEntity;
            this.instance = LazyOptional.of(() -> new TeamCapability(livingEntity));
        }
        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return ModCapability.TEAM.orEmpty(cap, instance);
        }

        @Override
        public Tag serializeNBT() {
            return TeamData.CODEC.encodeStart(NbtOps.INSTANCE, instance.orElse(new TeamCapability(livingEntity)).data).result().orElse(new CompoundTag());
        }

        @Override
        public void deserializeNBT(Tag compoundTag) {
            instance.ifPresent(teamCapability -> {
                TeamData parsed = TeamData.CODEC.parse(NbtOps.INSTANCE, compoundTag).result().orElse(
                        new TeamData(livingEntity.getType(), TeamUtil.WHITE, false, new Random().nextInt(2401) + 3600, false, Ingredient.of(Items.GOLDEN_APPLE), true)
                );
                parsed.setColor(TeamUtil.normalizeColor(parsed.getColor()));
                teamCapability.data.copy(parsed);
            });
        }
    }
}
