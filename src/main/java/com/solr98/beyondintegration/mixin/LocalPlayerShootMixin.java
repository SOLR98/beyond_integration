package com.solr98.beyondintegration.mixin;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.tacz.guns.client.gameplay.LocalPlayerShoot", remap = false)
public class LocalPlayerShootMixin {

    @Inject(method = "doShoot", at = @At("HEAD"))
    private void beyond$ensureMaxCount(GunDisplayInstance display, IGun iGun,
            ItemStack mainHandItem, GunData gunData, long delay, float chargeProgress, CallbackInfo ci) {
        if (iGun.useInventoryAmmo(mainHandItem) && iGun.getCurrentAmmoCount(mainHandItem) <= 0) {
            iGun.setCurrentAmmoCount(mainHandItem, 1);
        }
    }
}
