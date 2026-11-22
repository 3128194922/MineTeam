package com.xiaohunao.mine_team.common.network.s2c;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.xiaohunao.mine_team.common.capability.TeamCapability;
import com.xiaohunao.mine_team.common.capability.TeamData;
import com.xiaohunao.mine_team.common.util.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record TeamDataSyncS2CPayload(int entityId, TeamData data) {
    public static final Codec<TeamDataSyncS2CPayload> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("id").forGetter(TeamDataSyncS2CPayload::entityId),
            TeamData.CODEC.fieldOf("data").forGetter(TeamDataSyncS2CPayload::data)
    ).apply(instance, TeamDataSyncS2CPayload::new));

    public static void encode(TeamDataSyncS2CPayload packet, FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeJsonWithCodec(CODEC, packet);
    }
    public static TeamDataSyncS2CPayload decode(FriendlyByteBuf friendlyByteBuf) {
        return friendlyByteBuf.readJsonWithCodec(CODEC);
    }

    public static void handle(TeamDataSyncS2CPayload packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Client.handle(packet, ctx)));
        context.setPacketHandled(true);
    }

    public static class Client{
        public static void handle(TeamDataSyncS2CPayload packet, Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = ctx.get();
            context.enqueueWork(() -> {
                ClientLevel level = Minecraft.getInstance().level;
                Entity entity = level == null ? null : level.getEntity(packet.entityId);
                if (entity == null) {
                    LocalPlayer localPlayer = Minecraft.getInstance().player;
                    if (localPlayer != null && localPlayer.getId() == packet.entityId) {
                        entity = localPlayer;
                    }
                }
                if (!(entity instanceof LivingEntity livingEntity)) {
                    return;
                }

                String color = TeamUtil.normalizeColor(packet.data.getColor());
                TeamCapability.get(livingEntity).ifPresent(teamCapability -> {
                    teamCapability.data.copy(packet.data);
                    teamCapability.data.setColor(color);

                    Scoreboard scoreboard = livingEntity.level().getScoreboard();
                    PlayerTeam currentTeam = scoreboard.getPlayersTeam(livingEntity.getScoreboardName());
                    if (TeamUtil.isNoTeam(color)) {
                        if (currentTeam != null) {
                            scoreboard.removePlayerFromTeam(livingEntity.getScoreboardName(), currentTeam);
                        }
                    } else {
                        PlayerTeam targetTeam = scoreboard.getPlayerTeam(color);
                        if (targetTeam != null && currentTeam != targetTeam) {
                            scoreboard.addPlayerToTeam(livingEntity.getScoreboardName(), targetTeam);
                        }
                    }
                });
            });
        }
    }
}
