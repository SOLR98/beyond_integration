package com.solr98.beyondintegration.handler;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.init.ModItems;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.AbstractUnorderedStackHandler.TypeBucket;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import java.util.List;

public class TaczAmmoExtractor {

    public static ResourceLocation getAmmoId(ItemStack gunStack) {
        IGun iGun = IGun.getIGunOrNull(gunStack);
        if (iGun == null) return null;
        ResourceLocation gunId = iGun.getGunId(gunStack);
        if (gunId == null) return null;
        var opt = TimelessAPI.getCommonGunIndex(gunId);
        if (opt.isEmpty()) return null;
        return opt.get().getGunData().getAmmoId();
    }

    public static ResourceLocation getAmmoIdClient(ItemStack gunStack) {
        IGun iGun = IGun.getIGunOrNull(gunStack);
        if (iGun == null) return null;
        ResourceLocation gunId = iGun.getGunId(gunStack);
        if (gunId == null) return null;
        var opt = TimelessAPI.getClientGunIndex(gunId);
        if (opt.isEmpty()) return null;
        return opt.get().getGunData().getAmmoId();
    }

    public static int countAmmoInNetwork(ItemStack gunStack, DimensionsNet net) {
        ItemStack reference = getAmmoReference(gunStack);
        if (reference != null) {
            KeyAmount found = net.getUnifiedStorage().getStackByKey(new ItemStackKey(reference));
            long count = found.amount();
            if (count > 0) return (int) Math.min(count, Integer.MAX_VALUE);
        }
        if (hasCreativeAmmoBoxInNetwork(gunStack, net)) return Integer.MAX_VALUE;
        return 0;
    }

    public static int countAmmoInNetworkByAmmoId(ResourceLocation ammoId, DimensionsNet net) {
        if (ammoId == null || net == null) return 0;
        ItemStackKey key = buildAmmoKey(ammoId);
        if (key == null) return 0;
        KeyAmount found = net.getUnifiedStorage().getStackByKey(key);
        long count = found.amount();
        if (count > 0) return (int) Math.min(count, Integer.MAX_VALUE);
        if (hasCreativeAmmoBoxInNetwork(ammoId, net)) return Integer.MAX_VALUE;
        return 0;
    }

    // ── 玩家全网络查询（主网络优先，再查其他） ──

    public static int countAmmoFromAll(ServerPlayer player, ItemStack gunStack) {
        DimensionsNet primary = DimensionsNet.getPrimaryNetFromPlayer(player);
        if (primary != null) {
            int count = countAmmoInNetwork(gunStack, primary);
            if (count > 0) return count;
        }
        for (DimensionsNet net : DimensionsNet.getAllNetFromPlayer(player)) {
            if (net == primary) continue;
            int count = countAmmoInNetwork(gunStack, net);
            if (count > 0) return count;
        }
        return 0;
    }

    public static int tryConsumeFromAll(ServerPlayer player, ItemStack gunStack, int neededAmount) {
        DimensionsNet primary = DimensionsNet.getPrimaryNetFromPlayer(player);
        if (primary != null) {
            int taken = consumeAmmoDirectly(gunStack, neededAmount, primary);
            if (taken > 0) return taken;
        }
        for (DimensionsNet net : DimensionsNet.getAllNetFromPlayer(player)) {
            if (net == primary) continue;
            int taken = consumeAmmoDirectly(gunStack, neededAmount, net);
            if (taken > 0) return taken;
        }
        return 0;
    }

    // ── 女仆网络支持 ──

    public static int countAmmoFromMaid(LivingEntity entity, ItemStack gunStack) {
        if (!ModList.get().isLoaded("touhou_little_maid")) return 0;
        DimensionsNet net = com.solr98.beyondintegration.maid.MaidNetworkHelper.findTerminal(entity);
        if (net == null) return 0;
        return countAmmoInNetwork(gunStack, net);
    }

    public static int tryConsumeFromMaid(LivingEntity entity, ItemStack gunStack, int neededAmount) {
        if (!ModList.get().isLoaded("touhou_little_maid")) return 0;
        DimensionsNet net = com.solr98.beyondintegration.maid.MaidNetworkHelper.findTerminal(entity);
        if (net == null) return 0;
        return consumeAmmoDirectly(gunStack, neededAmount, net);
    }

    public static int consumeAmmoDirectly(ItemStack gunStack, int neededAmount, DimensionsNet net) {
        ItemStack reference = getAmmoReference(gunStack);
        if (reference != null) {
            KeyAmount extracted = net.getUnifiedStorage().extract(new ItemStackKey(reference), neededAmount, false, false);
            if (extracted.amount() > 0) {
                net.setDirty();
                return (int) extracted.amount();
            }
        }
        if (hasCreativeAmmoBoxInNetwork(gunStack, net)) return neededAmount;
        return 0;
    }

    public static boolean hasCreativeAmmoBoxInNetwork(ResourceLocation ammoId, DimensionsNet net) {
        if (ammoId == null || net == null) return false;
        var opt = net.getUnifiedStorage().getBucket(ItemStackKey.ID);
        if (opt.isEmpty()) return false;
        TypeBucket bucket = opt.get();
        for (int i = 0; i < bucket.size(); i++) {
            IStackKey<?> rawKey = bucket.get(i);
            if (!(rawKey instanceof ItemStackKey ik)) continue;
            ItemStack stack = ik.getReadOnlyStack();
            if (!(stack.getItem() instanceof IAmmoBox box)) continue;
            if (box.isAllTypeCreative(stack)) return true;
            if (box.isCreative(stack) && ammoId.equals(box.getAmmoId(stack))) return true;
        }
        return false;
    }

    public static boolean hasCreativeAmmoBoxInNetwork(ItemStack gunStack, DimensionsNet net) {
        ResourceLocation ammoId = getAmmoId(gunStack);
        return ammoId != null && hasCreativeAmmoBoxInNetwork(ammoId, net);
    }

    private static ItemStack getAmmoReference(ItemStack gunStack) {
        ResourceLocation ammoId = getAmmoId(gunStack);
        if (ammoId == null) return null;
        return buildAmmoStack(ammoId);
    }

    private static ItemStackKey buildAmmoKey(ResourceLocation ammoId) {
        ItemStack ref = buildAmmoStack(ammoId);
        return ref != null ? new ItemStackKey(ref) : null;
    }

    private static ItemStack buildAmmoStack(ResourceLocation ammoId) {
        ItemStack ref = new ItemStack(ModItems.AMMO.get());
        if (ref.getItem() instanceof IAmmo iAmmo) {
            iAmmo.setAmmoId(ref, ammoId);
        }
        return ref;
    }
}
