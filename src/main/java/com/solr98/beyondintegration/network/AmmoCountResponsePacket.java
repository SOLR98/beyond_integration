package com.solr98.beyondintegration.network;
import com.solr98.beyondintegration.BeyondIntegration;
import com.solr98.beyondintegration.client.TaczAmmoCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import java.util.LinkedHashMap;
import java.util.Map;

public record AmmoCountResponsePacket(ResourceLocation ammoId, Map<Integer, NetEntry> networks, boolean quick) implements CustomPacketPayload {
    public record NetEntry(int netId, String customName, int count) {}

    public static final Type<AmmoCountResponsePacket> TYPE = new Type<>(ResourceLocation.parse(BeyondIntegration.MODID + ":ammo_count_response"));
    public static final StreamCodec<FriendlyByteBuf, AmmoCountResponsePacket> STREAM_CODEC = new StreamCodec<>() {
        @Override public @NotNull AmmoCountResponsePacket decode(FriendlyByteBuf buf) {
            ResourceLocation ammoId = buf.readResourceLocation();
            boolean quick = buf.readBoolean();
            int size = buf.readVarInt();
            Map<Integer, NetEntry> networks = new LinkedHashMap<>(size);
            for (int i = 0; i < size; i++) {
                int netId = buf.readVarInt();
                String name = buf.readUtf(256);
                int count = buf.readVarInt();
                networks.put(netId, new NetEntry(netId, name, count));
            }
            return new AmmoCountResponsePacket(ammoId, networks, quick);
        }
        @Override public void encode(FriendlyByteBuf buf, AmmoCountResponsePacket p) {
            buf.writeResourceLocation(p.ammoId);
            buf.writeBoolean(p.quick);
            buf.writeVarInt(p.networks.size());
            for (NetEntry e : p.networks.values()) {
                buf.writeVarInt(e.netId);
                buf.writeUtf(e.customName, 256);
                buf.writeVarInt(e.count);
            }
        }
    };

    public static void handle(final AmmoCountResponsePacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().level().isClientSide())
                TaczAmmoCache.update(packet.ammoId, packet.networks, packet.quick);
        });
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
