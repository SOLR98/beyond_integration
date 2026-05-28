package com.solr98.beyondintegration.jade;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum VehicleClientProvider implements IEntityComponentProvider {
    INSTANCE;

    private static final String NET_ID_KEY = "Net_id";
    private static final String NET_NAME_KEY = "bce_net_name";

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (data == null || !data.contains(NET_ID_KEY)) return;
        int netId = data.getInt(NET_ID_KEY);
        String netName = data.getString(NET_NAME_KEY);
        String text = netName.isEmpty()
                ? "Net #" + netId
                : netName + " (#" + netId + ")";
        tooltip.add(Component.literal("[" + text + "]").withStyle(ChatFormatting.AQUA));
    }

    @Override
    public ResourceLocation getUid() { return ResourceLocation.parse("beyond_integration:vehicle_client"); }
}
