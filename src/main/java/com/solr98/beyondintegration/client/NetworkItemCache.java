package com.solr98.beyondintegration.client;
import net.minecraft.network.chat.Component;
import java.util.HashMap;
import java.util.Map;

public class NetworkItemCache {
    private static Map<String, Long> counts = new HashMap<>();
    private static boolean hasNetwork = true;
    private static int netId = -1;
    private static String netName = "";
    private static int version = 0;

    public static void setAll(Map<String, Long> data, boolean hasNet, int id, String name) {
        counts = new HashMap<>(data);
        hasNetwork = hasNet;
        netId = id;
        netName = name != null ? name : "";
        version++;
    }

    public static int getNetId() { return netId; }
    public static boolean hasNetwork() { return hasNetwork; }
    public static long getCount(String itemKey) { return counts.getOrDefault(itemKey, 0L); }
    public static int getVersion() { return version; }
    public static boolean isEmpty() { return counts.isEmpty(); }

    public static Component getDisplayName() {
        if (!hasNetwork) return Component.translatable("gui.beyond_integration.network.none");
        if (!netName.isEmpty())
            return Component.literal(netName + " (Net#" + netId + ")");
        return Component.translatable("gui.beyond_integration.network.connected", netId >= 0 ? netId : "?");
    }

    public static void clear() { counts.clear(); hasNetwork = true; netId = -1; netName = ""; }
}
