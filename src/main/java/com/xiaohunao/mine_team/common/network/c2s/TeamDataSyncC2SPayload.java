package com.xiaohunao.mine_team.common.network.c2s;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.xiaohunao.mine_team.common.capability.TeamCapability;
import com.xiaohunao.mine_team.common.capability.TeamData;
import com.xiaohunao.mine_team.common.team.TeamSync;
import com.xiaohunao.mine_team.common.util.TeamUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record TeamDataSyncC2SPayload(int entityId,TeamData data) {
    public static final Codec<TeamDataSyncC2SPayload> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("id").forGetter(TeamDataSyncC2SPayload::entityId),
            TeamData.CODEC.fieldOf("data").forGetter(TeamDataSyncC2SPayload::data)
    ).apply(instance, TeamDataSyncC2SPayload::new));
    public static void encode(TeamDataSyncC2SPayload packet, FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeJsonWithCodec(CODEC, packet);
    }
    public static TeamDataSyncC2SPayload decode(FriendlyByteBuf friendlyByteBuf) {
        return friendlyByteBuf.readJsonWithCodec(CODEC);
    }

    public static void handle(TeamDataSyncC2SPayload packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }

            TeamCapability capability = TeamCapability.get(sender).orElse(null);
            if (capability == null) {
                return;
            }

            String color = TeamUtil.normalizeColor(packet.data.getColor());
            boolean pvp = packet.data.isPvP();
            boolean colorChanged = !color.equals(TeamUtil.normalizeColor(capability.data.getColor()));
            boolean pvpChanged = pvp != capability.data.isPvP();

            if (colorChanged) {
                capability.data.setColor(color);
            }
            if (pvpChanged) {
                capability.data.setPvp(pvp);
            }

            boolean teamChanged = TeamSync.applyTeam(sender, color, false);
            if (colorChanged || teamChanged) {
                TeamSync.refreshOwnedPets(sender);
            }
            if (colorChanged || pvpChanged || teamChanged) {
                TeamSync.send(sender);
            }
        });
        context.setPacketHandled(true);
    }

}
