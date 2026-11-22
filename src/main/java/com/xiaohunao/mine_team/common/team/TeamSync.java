package com.xiaohunao.mine_team.common.team;

import com.xiaohunao.mine_team.MineTeam;
import com.xiaohunao.mine_team.common.capability.TeamCapability;
import com.xiaohunao.mine_team.common.capability.TeamData;
import com.xiaohunao.mine_team.common.config.MineTeamConfig;
import com.xiaohunao.mine_team.common.network.NetworkHandler;
import com.xiaohunao.mine_team.common.network.s2c.TeamDataSyncS2CPayload;
import com.xiaohunao.mine_team.common.util.TeamUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.network.PacketDistributor;

public final class TeamSync {
    private TeamSync() {
    }

    public static PlayerTeam ensureTeam(Scoreboard scoreboard, String color) {
        String normalized = TeamUtil.normalizeColor(color);
        PlayerTeam team = scoreboard.getPlayerTeam(normalized);
        if (team != null) {
            return team;
        }

        team = scoreboard.addPlayerTeam(normalized);
        ChatFormatting formatting = ChatFormatting.getByName(normalized);
        team.setColor(formatting == null ? ChatFormatting.WHITE : formatting);
        team.setDisplayName(Component.translatable(MineTeam.asResourceKey("team." + normalized)));
        team.setAllowFriendlyFire(MineTeamConfig.allowDamageSelf.get());
        return team;
    }

    public static void ensureTeams(Scoreboard scoreboard) {
        for (String color : TeamUtil.VALID_COLORS) {
            ensureTeam(scoreboard, color);
        }
    }

    public static boolean applyTeam(LivingEntity entity, String color, boolean sync) {
        String normalized = TeamUtil.normalizeColor(color);
        if (entity.level().isClientSide()) {
            TeamCapability.get(entity).ifPresent(capability -> capability.data.setColor(normalized));
            return false;
        }

        TeamCapability capability = TeamCapability.get(entity).orElse(null);
        if (capability == null) {
            return false;
        }

        Scoreboard scoreboard = entity.level().getScoreboard();
        PlayerTeam currentTeam = scoreboard.getPlayersTeam(entity.getScoreboardName());
        PlayerTeam targetTeam = TeamUtil.isNoTeam(normalized) ? null : ensureTeam(scoreboard, normalized);

        boolean colorChanged = !normalized.equals(TeamUtil.normalizeColor(capability.data.getColor()));
        boolean teamChanged = currentTeam != targetTeam;
        if (!colorChanged && !teamChanged) {
            return false;
        }

        capability.data.setColor(normalized);
        if (targetTeam == null) {
            if (currentTeam != null) {
                scoreboard.removePlayerFromTeam(entity.getScoreboardName(), currentTeam);
            }
        } else if (currentTeam != targetTeam) {
            scoreboard.addPlayerToTeam(entity.getScoreboardName(), targetTeam);
        }

        if (sync) {
            send(entity);
        }
        return true;
    }

    public static void send(LivingEntity entity) {
        TeamCapability.get(entity).ifPresent(capability -> {
            TeamData data = capability.data.copy();
            data.setColor(TeamUtil.normalizeColor(data.getColor()));
            NetworkHandler.CHANNEL.send(
                    PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
                    new TeamDataSyncS2CPayload(entity.getId(), data)
            );
        });
    }

    public static void syncPlayer(ServerPlayer player, boolean refreshPets) {
        MinecraftServer server = player.getServer();
        TeamCapability capability = TeamCapability.get(player).orElse(null);
        if (server == null || capability == null) {
            return;
        }

        Scoreboard scoreboard = server.getScoreboard();
        String color = TeamUtil.normalizeColor(capability.data.getColor());
        PlayerTeam currentTeam = scoreboard.getPlayersTeam(player.getScoreboardName());

        if (TeamUtil.isNoTeam(color) && currentTeam != null && !TeamUtil.isNoTeam(currentTeam.getName())) {
            color = TeamUtil.normalizeColor(currentTeam.getName());
            capability.data.setColor(color);
        }

        applyTeam(player, color, false);
        send(player);

        if (refreshPets) {
            refreshOwnedPets(player);
        }
    }

    public static void syncPet(LivingEntity pet, boolean sync) {
        if (pet.level().isClientSide()
                || pet instanceof Player
                || !TeamUtil.isPet(pet)
                || TeamUtil.isFrantic(pet)) {
            return;
        }

        Player owner = TeamUtil.getOwner(pet);
        if (owner == null) {
            return;
        }
        applyTeam(pet, TeamUtil.getEffectiveTeamColor(owner), sync);
    }

    public static void syncFromScoreboard(LivingEntity entity) {
        if (entity.level().isClientSide()) {
            return;
        }

        TeamCapability capability = TeamCapability.get(entity).orElse(null);
        PlayerTeam team = entity.level().getScoreboard().getPlayersTeam(entity.getScoreboardName());
        if (capability == null) {
            return;
        }
        if (team == null) {
            if (!TeamUtil.isFrantic(entity)) {
                capability.data.setColor(TeamUtil.WHITE);
            }
            return;
        }

        String color = team.getName();
        if (TeamUtil.isNoTeam(color)) {
            entity.level().getScoreboard().removePlayerFromTeam(entity.getScoreboardName(), team);
            capability.data.setColor(TeamUtil.WHITE);
        } else if (TeamUtil.isValidColor(color)) {
            capability.data.setColor(TeamUtil.normalizeColor(color));
        }
    }

    public static void refreshOwnedPets(ServerPlayer owner) {
        MinecraftServer server = owner.getServer();
        if (server == null) {
            return;
        }

        String ownerColor = TeamUtil.getEffectiveTeamColor(owner);
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof Mob living
                        && !TeamUtil.isFrantic(living)
                        && TeamUtil.isOwnedBy(living, owner.getUUID())) {
                    applyTeam(living, ownerColor, true);
                }
            }
        }
    }
}
