package com.solr98.beyondintegration.mixin;

import com.solr98.beyondintegration.handler.NetIdAccessor;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.AbstractUnorderedStackHandler;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.lang.reflect.Field;

@Mixin(targets = "com.wintercogs.beyonddimensions.common.menu.DimensionsNetMenu", remap = false)
public class DimensionsNetMenuMixin {
    @Shadow(remap = false) public AbstractUnorderedStackHandler storage;

    @Unique
    private static final Field beyond$NET_FIELD;

    static {
        Field f = null;
        try {
            f = UnifiedStorage.class.getDeclaredField("net");
            f.setAccessible(true);
        } catch (Exception ignored) {}
        beyond$NET_FIELD = f;
    }

    @Inject(method = "<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lcom/wintercogs/beyonddimensions/api/storage/handler/impl/AbstractUnorderedStackHandler;)V",
            at = @At("RETURN"), remap = false)
    private void onServerInit(MenuType<?> menuType, int id, Inventory inv, AbstractUnorderedStackHandler data, CallbackInfo ci) {
        if (data instanceof UnifiedStorage us) {
            try {
                Object net = beyond$NET_FIELD.get(us);
                if (net instanceof DimensionsNet dn)
                    ((NetIdAccessor) this).beyond$setNetId(dn.getId());
            } catch (Exception ignored) {}
        }
    }
}
