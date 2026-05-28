package com.solr98.beyondintegration.network;
import com.solr98.beyondintegration.BeyondIntegration;
import com.solr98.beyondintegration.client.SuperbAmmoCache;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import java.util.HashMap;
import java.util.Map;

public record SuperbAmmoStatusResponsePacket(int netId, Map<String, Long> ammoMap, long energy, int mode, String networkName, boolean enchantSeparation) implements CustomPacketPayload {
    public static final Type<SuperbAmmoStatusResponsePacket> TYPE = new Type<>(ResourceLocation.parse(BeyondIntegration.MODID + ":superb_ammo_status_response"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SuperbAmmoStatusResponsePacket> STREAM_CODEC = new StreamCodec<>() {
        @Override public @NotNull SuperbAmmoStatusResponsePacket decode(RegistryFriendlyByteBuf buf) {
            int netId = buf.readInt(); long energy = buf.readLong(); int mode = buf.readInt();
            String networkName = buf.readUtf(); int size = buf.readInt();
            Map<String, Long> map = new HashMap<>();
            for (int i = 0; i < size; i++) map.put(buf.readUtf(), buf.readLong());
            boolean enchantSep = buf.readBoolean();
            return new SuperbAmmoStatusResponsePacket(netId, map, energy, mode, networkName, enchantSep);
        }
        @Override public void encode(RegistryFriendlyByteBuf buf, SuperbAmmoStatusResponsePacket p) {
            buf.writeInt(p.netId); buf.writeLong(p.energy); buf.writeInt(p.mode);
            buf.writeUtf(p.networkName); buf.writeInt(p.ammoMap.size());
            p.ammoMap.forEach((k, v) -> { buf.writeUtf(k); buf.writeLong(v); });
            buf.writeBoolean(p.enchantSeparation);
        }
    };
    public static void handle(final SuperbAmmoStatusResponsePacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (packet.mode == 1) SuperbAmmoCache.INSTANCE.updateVehicle(packet.netId, packet.ammoMap, packet.energy, packet.networkName);
            else SuperbAmmoCache.INSTANCE.update(packet.netId, packet.ammoMap, packet.energy, packet.networkName);
            SuperbAmmoCache.INSTANCE.setEnchantSeparation(packet.enchantSeparation);
        });
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
