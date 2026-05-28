package com.solr98.beyondintegration.network;
import com.solr98.beyondintegration.client.CraftToast;
import com.solr98.beyondintegration.client.NetworkItemCache;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import java.util.HashMap;
import java.util.Map;

public record NetworkItemCountsPacket(Map<String, Long> counts, boolean replace, boolean hasNetwork, int netId,
                                       String netName, ItemStack resultItem, int resultCount) implements CustomPacketPayload {
    public static final Type<NetworkItemCountsPacket> TYPE = new Type<>(ResourceLocation.parse("beyond_integration:network_item_counts"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NetworkItemCountsPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override public @NotNull NetworkItemCountsPacket decode(RegistryFriendlyByteBuf buf) {
            boolean replace = buf.readBoolean();
            boolean hasNetwork = buf.readBoolean();
            int netId = buf.readVarInt();
            String netName = buf.readUtf(256);
            int size = buf.readVarInt();
            Map<String, Long> counts = new HashMap<>();
            for (int i = 0; i < size; i++) counts.put(buf.readUtf(), buf.readVarLong());
            boolean hasResult = buf.readBoolean();
            ItemStack resultItem = hasResult ? ItemStack.STREAM_CODEC.decode(buf) : ItemStack.EMPTY;
            int resultCount = hasResult ? buf.readVarInt() : 0;
            return new NetworkItemCountsPacket(counts, replace, hasNetwork, netId, netName, resultItem, resultCount);
        }
        @Override public void encode(RegistryFriendlyByteBuf buf, NetworkItemCountsPacket p) {
            buf.writeBoolean(p.replace);
            buf.writeBoolean(p.hasNetwork);
            buf.writeVarInt(p.netId);
            buf.writeUtf(p.netName, 256);
            buf.writeVarInt(p.counts.size());
            p.counts.forEach((k, v) -> { buf.writeUtf(k); buf.writeVarLong(v); });
            boolean hasResult = !p.resultItem.isEmpty();
            buf.writeBoolean(hasResult);
            if (hasResult) {
                ItemStack.STREAM_CODEC.encode(buf, p.resultItem);
                buf.writeVarInt(p.resultCount);
            }
        }
    };

    public NetworkItemCountsPacket(Map<String, Long> counts, boolean replace, boolean hasNetwork, int netId, String netName) {
        this(counts, replace, hasNetwork, netId, netName, ItemStack.EMPTY, 0);
    }

    public static void handle(final NetworkItemCountsPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            NetworkItemCache.setAll(packet.counts, packet.hasNetwork, packet.netId, packet.netName);
            if (!packet.resultItem.isEmpty())
                CraftToast.show(packet.resultItem, packet.resultCount);
        });
    }

    @Override public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }
}
