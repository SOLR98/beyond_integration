package com.solr98.beyondintegration.mixin;
import com.solr98.beyondintegration.handler.EnchantSeparationAccessor;
import com.solr98.beyondintegration.handler.NetworkAmmoData;
import com.solr98.beyondintegration.handler.NetworkNameProvider;
import com.solr98.beyondintegration.handler.SuperbAmmoAccessor;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import java.util.HashMap;
import java.util.Map;

@Mixin(targets = "com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet", remap = false)
public class DimensionsNetMixin implements SuperbAmmoAccessor, NetworkNameProvider, EnchantSeparationAccessor {
    @Unique private Map<String, Long> beyond$superbAmmo = new HashMap<>();
    @Unique private boolean beyond$ammoLoaded = false;
    @Unique private boolean beyond$enchantSeparation = true;

    @Unique
    private void beyond$loadFromDisk() {
        if (beyond$ammoLoaded) return;
        beyond$ammoLoaded = true;
        int netId = ((DimensionsNet) (Object) this).getId();
        if (netId < 0) return;
        NetworkAmmoData data = NetworkAmmoData.get();
        Map<String, Long> saved = data.getAmmoForNet(netId);
        if (!saved.isEmpty()) beyond$superbAmmo = new HashMap<>(saved);
        beyond$enchantSeparation = data.getEnchantSeparation(netId);
    }

    @Override
    public Map<String, Long> getSuperbAmmo() { beyond$loadFromDisk(); return beyond$superbAmmo; }

    @Override
    public void setSuperbAmmo(Map<String, Long> map) {
        beyond$loadFromDisk();
        beyond$superbAmmo.clear();
        if (map != null) beyond$superbAmmo.putAll(map);
    }

    @Override
    public boolean beyond$isEnchantSeparationEnabled() { beyond$loadFromDisk(); return beyond$enchantSeparation; }

    @Override
    public void beyond$setEnchantSeparationEnabled(boolean v) { beyond$enchantSeparation = v; }
}
