package com.solr98.beyondintegration.jade;
import net.neoforged.fml.ModList;
import snownee.jade.api.*;

@WailaPlugin
public class JadeBDPlugin implements IWailaPlugin {
    @Override public void register(IWailaCommonRegistration registration) {
        if (ModList.get().isLoaded("superbwarfare")) {
            try { registration.registerEntityDataProvider(VehicleServerProvider.INSTANCE, Class.forName("com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity").asSubclass(net.minecraft.world.entity.Entity.class)); }
            catch (ClassNotFoundException ignored) {}
        }
    }
    @Override public void registerClient(IWailaClientRegistration registration) {
        if (ModList.get().isLoaded("superbwarfare")) {
            try { registration.registerEntityComponent(VehicleClientProvider.INSTANCE, Class.forName("com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity").asSubclass(net.minecraft.world.entity.Entity.class)); }
            catch (ClassNotFoundException ignored) {}
        }
    }
}
