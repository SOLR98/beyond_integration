package com.solr98.beyondintegration.mixin;
import com.solr98.beyondintegration.handler.NetIdAccessor;
import com.wintercogs.beyonddimensions.common.menu.DimensionsNetMenu;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.wintercogs.beyonddimensions.common.menu.BDBaseMenu", remap = false)
public class BDBaseMenuMixin implements NetIdAccessor {
    @Unique private int beyond$netId = -1;

    @Override public int beyond$getNetId() { return beyond$netId; }
    @Override public void beyond$setNetId(int id) { beyond$netId = id; }

    @Inject(method = "shouldSendQuickData", at = @At("RETURN"), cancellable = true)
    private void onShouldSendQuickData(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        if ((Object) this instanceof DimensionsNetMenu && beyond$netId >= 0)
            cir.setReturnValue(true);
    }

    @Inject(method = "writeQuickDataTag", at = @At("HEAD"))
    private void onWriteQuickDataTag(CompoundTag tag, CallbackInfo ci) {
        if ((Object) this instanceof DimensionsNetMenu && beyond$netId >= 0)
            tag.putInt("beyond_net_id", beyond$netId);
    }

    @Inject(method = "readQuickDataTag", at = @At("HEAD"))
    private void onReadQuickDataTag(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("beyond_net_id"))
            beyond$netId = tag.getInt("beyond_net_id");
    }
}
