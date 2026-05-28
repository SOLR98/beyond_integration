package com.solr98.beyondintegration.mixin;
import com.solr98.beyondintegration.client.TaczAmmoCache;
import com.solr98.beyondintegration.handler.TaczAmmoExtractor;
import com.tacz.guns.api.item.IGun;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.tacz.guns.api.item.gun.AbstractGunItem", remap = false)
public class AbstractGunItemMixin {

    @Inject(method = "canReload", at = @At("RETURN"), cancellable = true)
    private void beyond$onCanReload(LivingEntity shooter, ItemStack gunItem, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null || iGun.useInventoryAmmo(gunItem)) return;

        if (shooter instanceof ServerPlayer sp) {
            if (TaczAmmoExtractor.countAmmoFromAll(sp, gunItem) > 0)
                cir.setReturnValue(true);
        } else if (shooter.level().isClientSide()) {
            clientCheck(gunItem, cir);
        } else {
            if (TaczAmmoExtractor.countAmmoFromMaid(shooter, gunItem) > 0)
                cir.setReturnValue(true);
        }
    }

    @Inject(method = "hasInventoryAmmo", at = @At("RETURN"), cancellable = true)
    private void beyond$onHasInventoryAmmo(LivingEntity shooter, ItemStack gun, boolean needCheckAmmo, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        IGun iGun = IGun.getIGunOrNull(gun);
        if (iGun == null || !iGun.useInventoryAmmo(gun)) return;

        if (shooter instanceof ServerPlayer sp) {
            if (TaczAmmoExtractor.countAmmoFromAll(sp, gun) > 0)
                cir.setReturnValue(true);
        } else if (shooter.level().isClientSide()) {
            clientCheck(gun, cir);
        } else {
            if (TaczAmmoExtractor.countAmmoFromMaid(shooter, gun) > 0)
                cir.setReturnValue(true);
        }
    }

    private static void clientCheck(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        ResourceLocation ammoId = TaczAmmoExtractor.getAmmoIdClient(stack);
        if (ammoId != null) {
            if (!TaczAmmoCache.hasData(ammoId))
                TaczAmmoCache.requestQuick(ammoId);
            if (!TaczAmmoCache.hasData(ammoId) || TaczAmmoCache.getCount(ammoId) > 0)
                cir.setReturnValue(true);
        } else {
            cir.setReturnValue(true);
        }
    }
}
