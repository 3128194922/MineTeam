package com.xiaohunao.mine_team.common.util;

import com.xiaohunao.mine_team.common.capability.TeamCapability;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class TeamUtil {
    public static final String WHITE = "white";

    public static final Set<String> VALID_COLORS = Collections.unmodifiableSet(
            Arrays.stream(ChatFormatting.values())
                    .filter(ChatFormatting::isColor)
                    .map(ChatFormatting::getName)
                    .collect(Collectors.toSet())
    );

    private TeamUtil() {
    }

    public static boolean isNoTeam(@Nullable String color) {
        return color == null || color.isEmpty() || WHITE.equalsIgnoreCase(color);
    }

    public static boolean isValidColor(@Nullable String color) {
        if (color == null) {
            return false;
        }
        return VALID_COLORS.contains(color) || VALID_COLORS.contains(color.toLowerCase(Locale.ROOT));
    }

    public static String normalizeColor(@Nullable String color) {
        if (color == null) {
            return WHITE;
        }
        if (VALID_COLORS.contains(color)) {
            return color;
        }
        String normalized = color.toLowerCase(Locale.ROOT);
        return VALID_COLORS.contains(normalized) ? normalized : WHITE;
    }

    @Nullable
    public static PlayerTeam getScoreboardTeam(LivingEntity entity) {
        Scoreboard scoreboard = entity.level().getScoreboard();
        return scoreboard.getPlayersTeam(entity.getScoreboardName());
    }

    @Nullable
    public static PlayerTeam getTeam(LivingEntity entity) {
        String color = getEffectiveTeamColor(entity);
        if (isNoTeam(color)) {
            return null;
        }
        return entity.level().getScoreboard().getPlayerTeam(color);
    }

    public static String getEffectiveTeamColor(LivingEntity entity) {
        if (entity instanceof Player) {
            PlayerTeam team = getScoreboardTeam(entity);
            if (team != null && isValidColor(team.getName())) {
                return normalizeColor(team.getName());
            }
            return getStoredColor(entity);
        }

        if (isPet(entity)) {
            Player owner = getOwner(entity);
            if (owner != null) {
                return getEffectiveTeamColor(owner);
            }
        }

        PlayerTeam team = getScoreboardTeam(entity);
        if (team != null && isValidColor(team.getName())) {
            return normalizeColor(team.getName());
        }
        return getStoredColor(entity);
    }

    public static String getStoredColor(LivingEntity entity) {
        return TeamCapability.get(entity)
                .map(capability -> normalizeColor(capability.data.getColor()))
                .orElse(WHITE);
    }

    public static boolean isSameTeam(LivingEntity first, LivingEntity second) {
        String firstColor = getEffectiveTeamColor(first);
        String secondColor = getEffectiveTeamColor(second);
        return !isNoTeam(firstColor) && !isNoTeam(secondColor) && firstColor.equals(secondColor);
    }

    @Nullable
    public static UUID getOwnerUUID(LivingEntity entity) {
        if (entity instanceof OwnableEntity ownable) {
            UUID owner = ownable.getOwnerUUID();
            if (owner != null) {
                return owner;
            }
        }

        CompoundTag data = entity.getPersistentData();
        String ownerName = data.getString("OwnerName");
        if (!ownerName.isEmpty()) {
            try {
                return UUID.fromString(ownerName);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    @Nullable
    public static Player getOwner(LivingEntity entity) {
        UUID ownerUUID = getOwnerUUID(entity);
        if (ownerUUID == null) {
            return null;
        }

        if (entity.level() instanceof ServerLevel serverLevel) {
            MinecraftServer server = serverLevel.getServer();
            if (server != null) {
                ServerPlayer player = server.getPlayerList().getPlayer(ownerUUID);
                if (player != null) {
                    return player;
                }
            }
        }
        return entity.level().getPlayerByUUID(ownerUUID);
    }

    public static boolean isOwnedBy(LivingEntity entity, UUID playerUUID) {
        UUID ownerUUID = getOwnerUUID(entity);
        return ownerUUID != null && ownerUUID.equals(playerUUID);
    }

    public static boolean isPet(LivingEntity entity) {
        return getOwnerUUID(entity) != null;
    }

    public static boolean isFrantic(LivingEntity entity) {
        return TeamCapability.get(entity)
                .map(capability -> capability.data.isFrantic())
                .orElse(false);
    }

    public static boolean isSitting(LivingEntity entity) {
        if (entity instanceof TamableAnimal tamable && tamable.isOrderedToSit()) {
            return true;
        }
        CompoundTag data = entity.getPersistentData();
        return data.contains("Sitting") && (data.getInt("Sitting") == 1 || data.getBoolean("Sitting"));
    }
}
