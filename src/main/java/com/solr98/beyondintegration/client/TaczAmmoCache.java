package com.solr98.beyondintegration.client;
import com.solr98.beyondintegration.network.AmmoCountResponsePacket;
import com.solr98.beyondintegration.network.PacketHandler;
import com.solr98.beyondintegration.network.RequestAmmoCountPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class TaczAmmoCache {
    private static final Map<String, Map<Integer, AmmoCountResponsePacket.NetEntry>> cache = new HashMap<>();
    private static final Set<String> responded = new HashSet<>();
    private static String pendingQuickId = null;
    private static String pendingFullId = null;
    private static long lastFullUpdateMs = 0;
    private static final long FULL_INTERVAL_MS = 6000;

    public static boolean hasData(ResourceLocation ammoId) {
        return responded.contains(ammoId.toString());
    }

    public static int getCount(ResourceLocation ammoId) {
        Map<Integer, AmmoCountResponsePacket.NetEntry> nets = cache.get(ammoId.toString());
        if (nets == null) return 0;
        long sum = nets.values().stream().mapToLong(e -> (long) e.count()).sum();
        return (int) Math.min(sum, Integer.MAX_VALUE);
    }

    public static Map<Integer, Integer> getAllNetworkCounts(ResourceLocation ammoId) {
        Map<Integer, AmmoCountResponsePacket.NetEntry> nets = cache.get(ammoId.toString());
        if (nets == null) return Collections.emptyMap();
        Map<Integer, Integer> result = new LinkedHashMap<>();
        nets.forEach((id, e) -> result.put(id, e.count()));
        return result;
    }

    public static Component getNetworkDisplayName(int netId, ResourceLocation ammoId) {
        Map<Integer, AmmoCountResponsePacket.NetEntry> nets = cache.get(ammoId.toString());
        if (nets == null) return Component.literal("Net#" + netId);
        AmmoCountResponsePacket.NetEntry entry = nets.get(netId);
        if (entry == null) return Component.literal("Net#" + netId);
        String name = entry.customName();
        if (name != null && !name.isEmpty())
            return Component.literal(name + " (Net#" + netId + ")");
        return Component.translatable("menu.text.beyonddimensions.net.default_name", netId);
    }

    public static void requestQuick(ResourceLocation ammoId) {
        String id = ammoId.toString();
        if (id.equals(pendingQuickId)) return;
        pendingQuickId = id;
        PacketHandler.sendToServer(new RequestAmmoCountPacket(ammoId, true));
    }

    public static void requestFull(ResourceLocation ammoId) {
        long now = System.currentTimeMillis();
        if (now - lastFullUpdateMs < FULL_INTERVAL_MS) return;
        String id = ammoId.toString();
        if (id.equals(pendingFullId)) return;
        pendingFullId = id;
        lastFullUpdateMs = now;
        PacketHandler.sendToServer(new RequestAmmoCountPacket(ammoId, false));
    }

    public static void update(ResourceLocation ammoId, Map<Integer, AmmoCountResponsePacket.NetEntry> networks, boolean quick) {
        cache.put(ammoId.toString(), new LinkedHashMap<>(networks));
        if (quick) pendingQuickId = null;
        else pendingFullId = null;
        responded.add(ammoId.toString());
    }

    public static void clear() {
        cache.clear();
        responded.clear();
        pendingQuickId = null;
        pendingFullId = null;
        lastFullUpdateMs = 0;
    }
}
