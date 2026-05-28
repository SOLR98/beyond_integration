package com.solr98.beyondintegration.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class CraftToast implements Toast {
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("toast/advancement");
    private final ItemStack result;
    private final int count;

    public CraftToast(ItemStack result, int count) {
        this.result = result;
        this.count = count;
    }

    @Override
    public Visibility render(GuiGraphics g, ToastComponent comp, long timer) {
        g.blitSprite(BACKGROUND, 0, 0, this.width(), this.height());
        g.renderFakeItem(result, 8, 8);
        g.drawString(comp.getMinecraft().font, "\u7F51\u7EDC\u5408\u6210", 30, 7, 0xFFFF5500);
        String desc = result.getHoverName().getString();
        if (count > 1) desc += " \u00D7" + count;
        g.drawString(comp.getMinecraft().font, desc, 30, 18, 0xFFFFFF);
        return timer >= 2500L ? Visibility.HIDE : Visibility.SHOW;
    }

    public static void show(ItemStack result, int count) {
        Minecraft.getInstance().getToasts().addToast(new CraftToast(result, count));
    }
}
