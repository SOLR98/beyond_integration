package com.solr98.beyondintegration.maid;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import net.minecraft.world.entity.LivingEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MaidNetworkCache {
    private static final Map<UUID, Integer> cache = new HashMap<>();
    private static final Map<UUID, Long> ts = new HashMap<>();
    private static final long TTL = 5000;

    public static DimensionsNet get(LivingEntity e) {
        Integer id = cache.get(e.getUUID());
        if (id == null) return null;
        Long t = ts.get(e.getUUID());
        if (t == null || System.currentTimeMillis() - t > TTL) { cache.remove(e.getUUID()); ts.remove(e.getUUID()); return null; }
        DimensionsNet n = DimensionsNet.getNetFromId(id);
        if (n == null || n.deleted) { cache.remove(e.getUUID()); ts.remove(e.getUUID()); return null; }
        return n;
    }
    public static void put(UUID u, int id) { cache.put(u, id); ts.put(u, System.currentTimeMillis()); }
    public static void remove(UUID u) { cache.remove(u); ts.remove(u); }
}
