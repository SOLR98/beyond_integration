package com.solr98.beyondintegration.mixin;
import com.atsuishio.superbwarfare.data.gun.AmmoConsumer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.tools.InventoryTool;
import com.solr98.beyondintegration.command.util.NetworkUtils;
import com.solr98.beyondintegration.handler.EnchantSeparationAccessor;
import com.solr98.beyondintegration.handler.SuperbAmmoAccessor;
import com.solr98.beyondintegration.handler.VehicleNetStorage;
import com.solr98.beyondintegration.maid.MaidNetworkHelper;
import com.solr98.beyondintegration.network.SuperbAmmoStatusResponsePacket;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EnergyStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mixin(targets = "com.atsuishio.superbwarfare.data.gun.AmmoConsumer", remap = false)
public class AmmoConsumerMixin {
    @Shadow(remap = false) private AmmoConsumer.AmmoConsumeType type;
    @Shadow(remap = false) private com.atsuishio.superbwarfare.data.gun.Ammo playerAmmoType;
    @Shadow(remap = false) private int loadAmount;
    @Shadow(remap = false) private ItemStack stack;
    @Unique private static final Set<UUID> beyond$notifiedPlayers = new HashSet<>();

    // ═══════════════════════════════════════════════════════
    //  PLAYER_AMMO 系列
    // ═══════════════════════════════════════════════════════

    @Inject(method = "consume(Lcom/atsuishio/superbwarfare/data/gun/GunData;Lnet/minecraft/world/entity/Entity;I)I",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void onConsumePlayer(GunData data, Entity entity, int loads, CallbackInfoReturnable<Integer> cir) {
        if (type != AmmoConsumer.AmmoConsumeType.PLAYER_AMMO) return;
        if (playerAmmoType == null) return;
        if (!(entity instanceof ServerPlayer player)) return;
        if (entity.level() == null || entity.level().isClientSide()) return;

        String key = playerAmmoType.serializationName;
        int need = loads * loadAmount;
        int personal = playerAmmoType.get(player);
        int fromPersonal = Math.min(personal, need);
        playerAmmoType.add(player, -fromPersonal);
        int remaining = need - fromPersonal;
        int consumed = fromPersonal / loadAmount;
        DimensionsNet usedNet = null;

        if (remaining > 0) {
            int fromInv = InventoryTool.consumeAmmoItem(player, playerAmmoType, remaining);
            consumed += fromInv / loadAmount;
            remaining -= fromInv;
        }

        if (remaining > 0) {
            for (var net : NetworkUtils.getPlayerNetsPrimaryFirst(player)) {
                if (!(net instanceof SuperbAmmoAccessor acc)) continue;
                var map = acc.getSuperbAmmo();
                if (map.getOrDefault("__infinite__", 0L) > 0) { consumed = loads; remaining = 0; usedNet = net; break; }
                long avail = map.getOrDefault(key, 0L);
                if (avail <= 0) continue;
                long take = Math.min(avail, remaining);
                map.put(key, avail - take);
                consumed += (int) (take / loadAmount);
                remaining -= (int) take;
                net.setDirty();
                usedNet = net;
                break;
            }
        }

        cir.setReturnValue(Math.min(consumed, loads));
        if (consumed > 0 && usedNet != null) {
            pushUpdate(player, usedNet);
            if (beyond$notifiedPlayers.add(player.getUUID())) {
                var primary = DimensionsNet.getPrimaryNetFromPlayer(player);
                int id = usedNet.getId();
                if (primary != null && primary.getId() == id)
                    player.sendSystemMessage(Component.translatable("message.beyond_integration.using_primary_net_ammo"));
                else
                    player.sendSystemMessage(Component.translatable("message.beyond_integration.using_net_ammo", id));
            }
        }
    }

    @Inject(method = "consume(Lcom/atsuishio/superbwarfare/data/gun/GunData;Lnet/minecraft/world/entity/Entity;I)I",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void onConsumeMaid(GunData data, Entity entity, int loads, CallbackInfoReturnable<Integer> cir) {
        if (type != AmmoConsumer.AmmoConsumeType.PLAYER_AMMO) return;
        if (playerAmmoType == null) return;
        if (entity instanceof ServerPlayer) return;
        if (entity instanceof VehicleEntity) return;
        if (!(entity instanceof LivingEntity living)) return;

        DimensionsNet net = MaidNetworkHelper.findTerminal(living);
        if (net == null || !(net instanceof SuperbAmmoAccessor acc)) return;

        var map = acc.getSuperbAmmo();
        String key = playerAmmoType.serializationName;
        long networkAmmo = map.getOrDefault(key, 0L);
        if (networkAmmo <= 0) {
            if (map.getOrDefault("__infinite__", 0L) > 0) cir.setReturnValue(loads);
            return;
        }
        int need = loads * loadAmount;
        long take = Math.min(networkAmmo, need);
        if (take <= 0) return;
        map.put(key, networkAmmo - take);
        int taken = (int) (take / loadAmount);
        if (taken > 0) cir.setReturnValue(Math.min(taken, loads));
    }

    @Inject(method = "consume(Lcom/atsuishio/superbwarfare/data/gun/GunData;Lnet/minecraft/world/entity/Entity;I)I",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void onConsumeVehicle(GunData data, Entity entity, int loads, CallbackInfoReturnable<Integer> cir) {
        if (type != AmmoConsumer.AmmoConsumeType.PLAYER_AMMO) return;
        if (playerAmmoType == null) return;
        if (!(entity instanceof VehicleEntity vehicle)) return;

        int boundNetId = VehicleNetStorage.getBoundNetId(vehicle.getUUID());
        if (boundNetId < 0) return;
        var net = DimensionsNet.getNetFromId(boundNetId);
        if (!(net instanceof SuperbAmmoAccessor acc)) return;

        var map = acc.getSuperbAmmo();
        String key = playerAmmoType.serializationName;
        long networkAmmo = map.getOrDefault(key, 0L);
        if (networkAmmo <= 0) {
            if (map.getOrDefault("__infinite__", 0L) > 0) cir.setReturnValue(loads);
            return;
        }
        int need = loads * loadAmount;
        long take = Math.min(networkAmmo, need);
        if (take <= 0) return;
        map.put(key, networkAmmo - take);
        net.setDirty();
        int taken = (int) (take / loadAmount);
        if (taken > 0) {
            cir.setReturnValue(taken);
            for (Entity p : vehicle.getPassengers()) {
                if (p instanceof ServerPlayer sp) pushVehicleUpdate(sp, net, boundNetId);
            }
        }
    }

    // ═══════════════════════════════════════════════════════
    //  ITEM 系列
    // ═══════════════════════════════════════════════════════

    @Inject(method = "consume(Lcom/atsuishio/superbwarfare/data/gun/GunData;Lnet/minecraft/world/entity/Entity;I)I",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void onConsumePlayerItem(GunData data, Entity entity, int loads, CallbackInfoReturnable<Integer> cir) {
        if (type != AmmoConsumer.AmmoConsumeType.ITEM) return;
        if (stack.isEmpty()) return;
        if (!(entity instanceof ServerPlayer player)) return;
        if (entity.level() == null || entity.level().isClientSide()) return;

        int need = loads * loadAmount;
        long taken = 0;
        var cap = player.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.ENTITY);
        if (cap != null) taken = InventoryTool.consumeItem(cap, stack.getItem(), need);

        if (taken < need) {
            var itemKey = new ItemStackKey(stack);
            for (var net : NetworkUtils.getPlayerNetsPrimaryFirst(player)) {
                var ext = net.getUnifiedStorage().extract(itemKey, need - (int) taken, false, false);
                if (ext.amount() > 0) { taken += ext.amount(); net.setDirty(); }
                if (taken >= need) break;
            }
        }
        if (taken > 0) cir.setReturnValue((int) (taken / loadAmount));
    }

    @Inject(method = "consume(Lcom/atsuishio/superbwarfare/data/gun/GunData;Lnet/minecraft/world/entity/Entity;I)I",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void onConsumeVehicleItem(GunData data, Entity entity, int loads, CallbackInfoReturnable<Integer> cir) {
        if (type != AmmoConsumer.AmmoConsumeType.ITEM) return;
        if (stack.isEmpty()) return;
        if (!(entity instanceof VehicleEntity vehicle)) return;

        int netId = VehicleNetStorage.getBoundNetId(vehicle.getUUID());
        if (netId < 0) return;
        var net = DimensionsNet.getNetFromId(netId);
        if (net == null) return;

        int need = loads * loadAmount;
        var ext = net.getUnifiedStorage().extract(new ItemStackKey(stack), need, false, false);
        if (ext.amount() > 0) { net.setDirty(); cir.setReturnValue((int) (ext.amount() / loadAmount)); }
    }

    @Inject(method = "consume(Lcom/atsuishio/superbwarfare/data/gun/GunData;Lnet/minecraft/world/entity/Entity;I)I",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void onConsumeMaidItem(GunData data, Entity entity, int loads, CallbackInfoReturnable<Integer> cir) {
        if (type != AmmoConsumer.AmmoConsumeType.ITEM) return;
        if (stack.isEmpty()) return;
        if (!(entity instanceof LivingEntity living)) return;
        if (!net.neoforged.fml.ModList.get().isLoaded("touhou_little_maid")) return;

        var maidNet = MaidNetworkHelper.findTerminal(living);
        if (maidNet == null) return;
        int need = loads * loadAmount;
        var ext = maidNet.getUnifiedStorage().extract(new ItemStackKey(stack), need, false, false);
        if (ext.amount() > 0) { maidNet.setDirty(); cir.setReturnValue((int) (ext.amount() / loadAmount)); }
    }

    // ═══════════════════════════════════════════════════════
    //  推送方法
    // ═══════════════════════════════════════════════════════

    private static void pushUpdate(ServerPlayer player, DimensionsNet net) {
        if (!(net instanceof SuperbAmmoAccessor acc)) return;
        var ammo = acc.getSuperbAmmo();
        if (ammo.isEmpty()) return;
        long energy = net.getUnifiedStorage().getStackByKey(EnergyStackKey.INSTANCE).amount();
        String name = net instanceof com.solr98.beyondintegration.handler.NetworkNameProvider nnp ? nnp.getCustomName() : "";
        boolean enchantSep = !(net instanceof EnchantSeparationAccessor ea) || ea.beyond$isEnchantSeparationEnabled();
        PacketDistributor.sendToPlayer(player, new SuperbAmmoStatusResponsePacket(
                net.getId(), new HashMap<>(ammo), energy, 0, name, enchantSep));
    }

    private static void pushVehicleUpdate(ServerPlayer player, DimensionsNet net, int netId) {
        if (!(net instanceof SuperbAmmoAccessor acc)) return;
        long energy = net.getUnifiedStorage().getStackByKey(EnergyStackKey.INSTANCE).amount();
        String name = net instanceof com.solr98.beyondintegration.handler.NetworkNameProvider nnp ? nnp.getCustomName() : "";
        boolean enchantSep = !(net instanceof EnchantSeparationAccessor ea) || ea.beyond$isEnchantSeparationEnabled();
        PacketDistributor.sendToPlayer(player, new SuperbAmmoStatusResponsePacket(
                netId, new HashMap<>(acc.getSuperbAmmo()), energy, 1, name, enchantSep));
    }
}
