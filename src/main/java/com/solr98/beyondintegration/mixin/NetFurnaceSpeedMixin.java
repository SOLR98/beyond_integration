package com.solr98.beyondintegration.mixin;

import com.wintercogs.beyonddimensions.common.block.entity.BaseNetFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.wintercogs.beyonddimensions.common.block.entity.BaseNetFurnaceBlockEntity", remap = false)
public class NetFurnaceSpeedMixin {

    @Inject(method = "workContent", at = @At("TAIL"))
    private void onWorkContent(CallbackInfo ci) {
        var self = (BaseNetFurnaceBlockEntity<?>) (Object) this;
        var lit = self.getLitTime();
        var cook = self.getCookTime();
        var total = self.getCookTimeTotal();
        for (int i = 0; i < cook.size(); i++) {
            if (lit.get(i) > 0)
                lit.set(i, Math.max(0, lit.get(i) - 63));
            if (cook.get(i) > 0 && cook.get(i) <= total.get(i))
                cook.set(i, Math.min(total.get(i), cook.get(i) + 63));
        }
    }
}
