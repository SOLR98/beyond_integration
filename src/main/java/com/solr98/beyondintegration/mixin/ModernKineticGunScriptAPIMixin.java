package com.solr98.beyondintegration.mixin;
import com.solr98.beyondintegration.client.TaczAmmoCache;
import com.solr98.beyondintegration.handler.TaczAmmoExtractor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.tacz.guns.item.ModernKineticGunScriptAPI", remap = false)
public class ModernKineticGunScriptAPIMixin {
    @Shadow(remap = false) private LivingEntity shooter;
    @Shadow(remap = false) private ItemStack itemStack;

    @Inject(method = "consumeAmmoFromPlayer", at = @At("RETURN"), cancellable = true)
    private void beyond$onConsumeAmmoFromPlayer(int neededAmount, CallbackInfoReturnable<Integer> cir) {
        int found = cir.getReturnValue();
        if (found >= neededAmount) return;

        if (shooter instanceof ServerPlayer sp) {
            int fromNet = TaczAmmoExtractor.tryConsumeFromAll(sp, itemStack, neededAmount - found);
            if (fromNet > 0) cir.setReturnValue(found + fromNet);
        } else {
            int fromNet = TaczAmmoExtractor.tryConsumeFromMaid(shooter, itemStack, neededAmount - found);
            if (fromNet > 0) cir.setReturnValue(found + fromNet);
        }
    }

    @Inject(method = "hasAmmoToConsume", at = @At("RETURN"), cancellable = true)
    private void beyond$onHasAmmoToConsume(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;

        if (shooter instanceof ServerPlayer sp) {
            if (TaczAmmoExtractor.countAmmoFromAll(sp, itemStack) > 0)
                cir.setReturnValue(true);
        } else if (shooter.level().isClientSide()) {
            ResourceLocation ammoId = TaczAmmoExtractor.getAmmoIdClient(itemStack);
            if (ammoId != null) {
                if (!TaczAmmoCache.hasData(ammoId))
                    TaczAmmoCache.requestQuick(ammoId);
                if (!TaczAmmoCache.hasData(ammoId) || TaczAmmoCache.getCount(ammoId) > 0)
                    cir.setReturnValue(true);
            } else {
                cir.setReturnValue(true);
            }
        } else if (TaczAmmoExtractor.countAmmoFromMaid(shooter, itemStack) > 0) {
            cir.setReturnValue(true);
        }
    }
}
