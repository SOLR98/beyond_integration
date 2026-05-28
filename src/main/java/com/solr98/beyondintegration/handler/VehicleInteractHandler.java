package com.solr98.beyondintegration.handler;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class VehicleInteractHandler {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        try {
            Class<?> vehicleClass = Class.forName("com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity");
            if (!vehicleClass.isInstance(event.getTarget())) return;
        } catch (Exception ignored) { return; }
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        int netId = stack.getOrDefault(com.wintercogs.beyonddimensions.common.init.BDDataComponents.NET_ID_DATA, -1);
        if (netId < 0) return;
        event.setCanceled(true);
    }
}
