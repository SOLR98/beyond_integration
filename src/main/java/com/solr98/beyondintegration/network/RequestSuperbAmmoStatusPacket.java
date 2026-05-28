package com.solr98.beyondintegration.network;
import com.solr98.beyondintegration.BeyondIntegration;
import com.solr98.beyondintegration.handler.EnchantSeparationAccessor;
import com.solr98.beyondintegration.handler.SuperbAmmoAccessor;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EnergyStackKey;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import java.util.HashMap;

public record RequestSuperbAmmoStatusPacket(int netId) implements CustomPacketPayload {
    public static final Type<RequestSuperbAmmoStatusPacket> TYPE = new Type<>(ResourceLocation.parse(BeyondIntegration.MODID + ":request_superb_ammo_status"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestSuperbAmmoStatusPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override public @NotNull RequestSuperbAmmoStatusPacket decode(RegistryFriendlyByteBuf buf) { return new RequestSuperbAmmoStatusPacket(buf.readInt()); }
        @Override public void encode(RegistryFriendlyByteBuf buf, RequestSuperbAmmoStatusPacket p) { buf.writeInt(p.netId); }
    };

    public static void handle(final RequestSuperbAmmoStatusPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            DimensionsNet net = null;
            if (packet.netId >= 0) net = DimensionsNet.getNetFromId(packet.netId);
            if (net == null) net = DimensionsNet.getPrimaryNetFromPlayer(player);
            if (net == null || !(net instanceof SuperbAmmoAccessor acc)) return;
            var map = acc.getSuperbAmmo();
            if (map.isEmpty()) return;
            long energy = net.getUnifiedStorage().getStackByKey(EnergyStackKey.INSTANCE).amount();
            boolean enchantSep = !(net instanceof EnchantSeparationAccessor ea) || ea.beyond$isEnchantSeparationEnabled();
            String name = net instanceof com.solr98.beyondintegration.handler.NetworkNameProvider nnp ? nnp.getCustomName() : "";
            PacketDistributor.sendToPlayer(player, new SuperbAmmoStatusResponsePacket(
                    net.getId(), new HashMap<>(map), energy, 0, name, enchantSep));
        });
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
