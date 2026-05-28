package com.solr98.beyondintegration.mixin;
import com.atsuishio.superbwarfare.data.gun.AmmoConsumer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.solr98.beyondintegration.client.SuperbAmmoCache;
import com.solr98.beyondintegration.command.util.NetworkUtils;
import com.solr98.beyondintegration.handler.SuperbAmmoAccessor;
import com.solr98.beyondintegration.handler.VehicleNetStorage;
import com.solr98.beyondintegration.maid.MaidNetworkHelper;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.ArrayList;
import java.util.List;

@Mixin(targets = "com.atsuishio.superbwarfare.data.gun.GunData", remap = false)
public class GunDataMixin {


    private boolean beyond$isBackpackMode() {
        try { return ((GunData) (Object) this).useBackpackAmmo(); } catch (Exception e) { return false; }
    }

    @Inject(method = "hasBackupAmmo", at = @At("RETURN"), cancellable = true, remap = false)
    private void onHasBackupAmmo(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        try {
            var consumer = ((GunData) (Object) this).selectedAmmoConsumer();
            if (consumer == null) return;
            var type = consumer.getType();
            if (type == AmmoConsumer.AmmoConsumeType.PLAYER_AMMO) {
                var ammoType = consumer.getPlayerAmmoType();
                if (ammoType == null) return;
                String key = ammoType.serializationName;
                if (SuperbAmmoCache.INSTANCE.hasData() && SuperbAmmoCache.INSTANCE.hasInfinite()) { cir.setReturnValue(true); return; }
                if (entity instanceof VehicleEntity && SuperbAmmoCache.INSTANCE.vehicleHasData() && SuperbAmmoCache.INSTANCE.getVehicleCount(key) > 0) { cir.setReturnValue(true); return; }
                for (var net : getNets(entity))
                    if (net instanceof SuperbAmmoAccessor acc && acc.getSuperbAmmo().getOrDefault(key, 0L) > 0) { cir.setReturnValue(true); return; }
                if (SuperbAmmoCache.INSTANCE.hasData() && SuperbAmmoCache.INSTANCE.getCount(key) > 0) cir.setReturnValue(true);
            } else if (type == AmmoConsumer.AmmoConsumeType.ITEM) {
                ItemStack ammoStack = consumer.stack();
                if (ammoStack.isEmpty()) return;
                for (var net : getNets(entity))
                    if (countItems(net, ammoStack) > 0) { cir.setReturnValue(true); return; }
            }
        } catch (Exception ignored) {}
    }

    @Inject(method = "countBackupAmmo", at = @At("RETURN"), cancellable = true, remap = false)
    private void onCountBackupAmmo(Entity entity, CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValue() >= Integer.MAX_VALUE / 2) return;
        // 客户端 HUD: 只对 useBackpackAmmo 枪显示网络弹药
        if (entity != null && entity.level() != null && entity.level().isClientSide() && !beyond$isBackpackMode()) return;
        try {
            var consumer = ((GunData) (Object) this).selectedAmmoConsumer();
            if (consumer == null) return;
            var type = consumer.getType();
            if (type == AmmoConsumer.AmmoConsumeType.PLAYER_AMMO) {
                var ammoType = consumer.getPlayerAmmoType();
                if (ammoType == null) return;
                String key = ammoType.serializationName;
                if (SuperbAmmoCache.INSTANCE.hasData() && SuperbAmmoCache.INSTANCE.hasInfinite()) { cir.setReturnValue(Integer.MAX_VALUE); return; }
                if (entity instanceof VehicleEntity && SuperbAmmoCache.INSTANCE.vehicleHasData() && SuperbAmmoCache.INSTANCE.getVehicleCount(key) > 0) { cir.setReturnValue((int) Math.min(cir.getReturnValue() + SuperbAmmoCache.INSTANCE.getVehicleCount(key), Integer.MAX_VALUE)); return; }
                long total = 0;
                for (var net : getNets(entity))
                    if (net instanceof SuperbAmmoAccessor acc) total += acc.getSuperbAmmo().getOrDefault(key, 0L);
                if (SuperbAmmoCache.INSTANCE.hasData()) total += SuperbAmmoCache.INSTANCE.getCount(key);
                if (total > 0) cir.setReturnValue((int) Math.min(cir.getReturnValue() + total, Integer.MAX_VALUE));
            } else if (type == AmmoConsumer.AmmoConsumeType.ITEM) {
                ItemStack ammoStack = consumer.stack();
                if (ammoStack.isEmpty()) return;
                long total = 0;
                for (var net : getNets(entity)) total += countItems(net, ammoStack);
                if (total > 0) cir.setReturnValue((int) Math.min(cir.getReturnValue() + total, Integer.MAX_VALUE));
            }
        } catch (Exception ignored) {}
    }

    private static long countItems(DimensionsNet net, ItemStack target) {
        var opt = net.getUnifiedStorage().getBucket(ItemStackKey.ID);
        if (opt.isEmpty()) return 0;
        var bucket = opt.get();
        var targetKey = new ItemStackKey(target);
        long total = 0;
        for (int i = 0; i < bucket.size(); i++) {
            if (bucket.get(i) instanceof ItemStackKey ik && ik.isSame(targetKey))
                total += net.getUnifiedStorage().getStackByKey(ik).amount();
        }
        return total;
    }

    private static List<DimensionsNet> getNets(Entity entity) {
        List<DimensionsNet> nets = new ArrayList<>();
        if (entity instanceof ServerPlayer p) nets.addAll(NetworkUtils.getPlayerNetsPrimaryFirst(p));
        if (entity instanceof VehicleEntity v) { int id = VehicleNetStorage.getBoundNetId(v.getUUID()); if (id >= 0) { var n = DimensionsNet.getNetFromId(id); if (n != null) nets.add(n); } }
        if (entity instanceof LivingEntity living && net.neoforged.fml.ModList.get().isLoaded("touhou_little_maid")) {
            var n = MaidNetworkHelper.findTerminal(living);
            if (n != null) nets.add(n);
        }
        return nets;
    }
}
