package com.solr98.beyondintegration.maid;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.IItemHandler;

public class MaidNetworkHelper {
    public static DimensionsNet findTerminal(LivingEntity entity) {
        if (!ModList.get().isLoaded("touhou_little_maid")) return null;
        var cached = MaidNetworkCache.get(entity);
        if (cached != null) return cached;
        try {
            if (!(entity instanceof com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid maid)) return null;
            DimensionsNet net = scanInv(maid.getMaidBauble());
            if (net != null) return cache(entity, net);
            MaidNetworkCache.remove(maid.getUUID());
        } catch (NoClassDefFoundError ignored) {}
        return null;
    }
    private static DimensionsNet scanInv(IItemHandler inv) {
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack st = inv.getStackInSlot(i);
            if (!st.isEmpty()) {
                int id = st.getOrDefault(BDDataComponents.NET_ID_DATA.get(), -1);
                if (id >= 0) { DimensionsNet n = DimensionsNet.getNetFromId(id); if (n != null) return n; }
            }
        }
        return null;
    }
    private static DimensionsNet cache(LivingEntity e, DimensionsNet n) { MaidNetworkCache.put(e.getUUID(), n.getId()); return n; }
}
