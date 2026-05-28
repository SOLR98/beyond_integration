package com.solr98.beyondintegration.mixin;
import com.solr98.beyondintegration.client.NetworkItemCache;
import com.solr98.beyondintegration.network.PacketHandler;
import com.solr98.beyondintegration.network.RequestNetworkItemsPacket;
import com.solr98.beyondintegration.network.TaczCraftPacket;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import com.tacz.guns.client.gui.components.smith.ImageButton;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.inventory.GunSmithTableMenu;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Properties;

@Mixin(targets = "com.tacz.guns.client.gui.GunSmithTableScreen", remap = false)
public abstract class GunSmithTableScreenMixin extends AbstractContainerScreen<GunSmithTableMenu> {

    protected GunSmithTableScreenMixin(GunSmithTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Unique private boolean beyond$outputToNetwork = false;
    @Unique private int beyond$netVersion = -1;
    @Unique private int[] beyond$cachedNetworkCounts;
    @Unique private String beyond$lastRecipeKey;
    @Unique private Button beyond$outputBtn;

    @Shadow(remap = false) private @Nullable Int2IntArrayMap playerIngredientCount;
    @Shadow(remap = false) private @Nullable RecipeHolder<GunSmithTableRecipe> selectedRecipe;
    @Shadow(remap = false) private void getPlayerIngredientCount(RecipeHolder<GunSmithTableRecipe> holder) {}

    @Unique private static final ResourceLocation beyond$tex = ResourceLocation.parse("tacz:textures/gui/gun_smith_table.png");
    @Unique private static ItemStack beyond$netIcon = ItemStack.EMPTY;
    @Unique private static File beyond$prefsFile;
    @Unique private static boolean beyond$loaded = false;

    @Unique
    private void beyond$loadPrefs() {
        try {
            if (beyond$prefsFile == null)
                beyond$prefsFile = new File(Minecraft.getInstance().gameDirectory, "config/beyond_integration_smith.properties");
            if (beyond$prefsFile.exists()) {
                var props = new Properties();
                try (var in = new FileInputStream(beyond$prefsFile)) {
                    props.load(in);
                    beyond$outputToNetwork = Boolean.parseBoolean(props.getProperty("outputToNetwork", "false"));
                }
            }
        } catch (Exception ignored) {}
    }

    @Unique
    private void beyond$savePrefs() {
        try {
            if (beyond$prefsFile == null)
                beyond$prefsFile = new File(Minecraft.getInstance().gameDirectory, "config/beyond_integration_smith.properties");
            beyond$prefsFile.getParentFile().mkdirs();
            var props = new Properties();
            props.setProperty("outputToNetwork", String.valueOf(beyond$outputToNetwork));
            try (var out = new FileOutputStream(beyond$prefsFile)) { props.store(out, "Beyond Integration Smith GUI"); }
        } catch (Exception ignored) {}
    }

    @Unique
    private static void beyond$initIcons() {
        if (!beyond$netIcon.isEmpty()) return;
        try {
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse("beyonddimensions:net_creater"));
            if (item != null) beyond$netIcon = new ItemStack(item);
        } catch (Exception ignored) {}
    }

    @Unique
    private void beyond$drawButton(GuiGraphics g, int x, int y, int mx, int my, ItemStack icon) {
        boolean hover = mx >= x && mx < x + 18 && my >= y && my < y + 18;
        int v = hover ? 182 : 164;
        g.blit(beyond$tex, x, y, 18, 18, 138, v, 48, 18, 256, 256);
        if (!icon.isEmpty()) g.renderFakeItem(icon, x + 1, y + 1);
    }

    @Unique
    private void beyond$refreshCounts() {
        if (selectedRecipe != null) getPlayerIngredientCount(selectedRecipe);
    }

    @Inject(method = "render", at = @At("TAIL"), remap = true)
    private void beyond$onRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!beyond$loaded) { beyond$loaded = true; beyond$loadPrefs(); }
        beyond$initIcons();

        var self = (GunSmithTableScreen) (Object) this;
        var font = Minecraft.getInstance().font;
        int left = self.getGuiLeft(), top = self.getGuiTop();

        Component netText = NetworkItemCache.getDisplayName();
        graphics.drawString(font, netText, left + 254, top + 33,
                NetworkItemCache.hasNetwork() ? 0x55FFFF : 0x888888, false);

        if (NetworkItemCache.hasNetwork()) {
            var outIcon = beyond$outputToNetwork ? beyond$netIcon : new ItemStack(net.minecraft.world.item.Items.CHEST);
            beyond$drawButton(graphics, left + 267, top + 162, mouseX, mouseY, outIcon);
        }
    }

    @Inject(method = "renderIngredient", at = @At("RETURN"))
    private void beyond$onRenderIngredient(GuiGraphics graphics, CallbackInfo ci) {
        if (!NetworkItemCache.hasNetwork()) return;
        if (selectedRecipe == null) return;
        var inputs = selectedRecipe.value().getInputs();
        if (inputs == null || inputs.isEmpty()) return;

        int netVer = NetworkItemCache.getVersion();
        if (beyond$cachedNetworkCounts == null || beyond$netVersion != netVer) {
            if (NetworkItemCache.isEmpty()) return;
            beyond$netVersion = netVer;
            beyond$cachedNetworkCounts = beyond$calcNetworkCounts(selectedRecipe.id().toString(), selectedRecipe.value());
            beyond$lastRecipeKey = selectedRecipe.id().toString();
        }

        var font = Minecraft.getInstance().font;
        var screen = (GunSmithTableScreen) (Object) this;
        int idx = 0;
        for (int i = 0; i < 6 && idx < inputs.size(); i++) {
            for (int j = 0; j < 2 && idx < inputs.size(); j++) {
                if (idx >= beyond$cachedNetworkCounts.length) break;
                int netCount = beyond$cachedNetworkCounts[idx];
                if (netCount > 0) {
                    int offsetX = screen.getGuiLeft() + 254 + 45 * j;
                    int offsetY = screen.getGuiTop() + 62 + 17 * i;
                    var pose = graphics.pose();
                    pose.pushPose();
                    pose.translate(0, 0, 250);
                    pose.scale(0.5f, 0.5f, 1);
                    graphics.drawString(font, "+" + netCount, (offsetX + 17) * 2, (offsetY + 5) * 2, 0x55FFFF, false);
                    pose.popPose();
                }
                idx++;
            }
        }
    }

    @Inject(method = "getPlayerIngredientCount", at = @At("RETURN"))
    private void beyond$addNetworkCounts(RecipeHolder<GunSmithTableRecipe> holder, CallbackInfo ci) {
        if (playerIngredientCount == null) return;

        PacketHandler.sendToServer(new RequestNetworkItemsPacket());

        if (!NetworkItemCache.hasNetwork() || NetworkItemCache.isEmpty()) return;

        var recipe = holder.value();
        int netVer = NetworkItemCache.getVersion();
        String recipeKey = holder.id().toString();
        boolean cacheStale = !recipeKey.equals(beyond$lastRecipeKey) || beyond$netVersion != netVer;

        if (cacheStale) {
            beyond$netVersion = netVer;
            beyond$lastRecipeKey = recipeKey;
            beyond$cachedNetworkCounts = beyond$calcNetworkCounts(holder.id().toString(), recipe);
        }

        int max = Math.min(beyond$cachedNetworkCounts.length, playerIngredientCount.size());
        for (int i = 0; i < max; i++) {
            int net = beyond$cachedNetworkCounts[i];
            if (net > 0) {
                long before = playerIngredientCount.get(i);
                long after = Math.min(before + net, Integer.MAX_VALUE);
                playerIngredientCount.put(i, (int) after);
            }
        }
    }

    @ModifyArg(method = "addCraftButton",
               at = @At(value = "INVOKE",
                        target = "Lcom/tacz/guns/client/gui/components/smith/ImageButton;<init>(IIIIIIILnet/minecraft/resources/ResourceLocation;Lnet/minecraft/client/gui/components/Button$OnPress;)V"),
               index = 8,
               remap = false)
    private Button.OnPress beyond$wrapOnPress(Button.OnPress original) {
        return b -> {
            if (selectedRecipe == null || playerIngredientCount == null) { original.onPress(b); return; }
            if (NetworkItemCache.hasNetwork()) {
                int count = Screen.hasShiftDown() ? 64 : 1;
                PacketHandler.sendToServer(new TaczCraftPacket(selectedRecipe.id(), count, beyond$outputToNetwork));
            } else {
                original.onPress(b);
            }
        };
    }

    @Inject(method = "addCraftButton", at = @At("TAIL"))
    private void beyond$onAddCraftButton(CallbackInfo ci) {
        var self = (GunSmithTableScreen) (Object) this;
        int left = self.getGuiLeft(), top = self.getGuiTop();

        beyond$outputBtn = addRenderableWidget(Button.builder(Component.empty(), b -> {
            beyond$outputToNetwork = !beyond$outputToNetwork;
            b.setTooltip(Tooltip.create(Component.translatable(beyond$outputToNetwork
                    ? "gui.beyond_integration.output.network"
                    : "gui.beyond_integration.output.inventory",
                    NetworkItemCache.getDisplayName())));
        }).bounds(left + 267, top + 162, 18, 18).build());
        beyond$outputBtn.visible = NetworkItemCache.hasNetwork();
    }

    @Inject(method = "onClose", at = @At("HEAD"), remap = true)
    private void beyond$onClose(CallbackInfo ci) {
        beyond$savePrefs();
    }

    @Unique
    private int[] beyond$calcNetworkCounts(String recipeId, GunSmithTableRecipe recipe) {
        var inputs = recipe.getInputs();
        if (inputs == null || inputs.isEmpty()) return new int[0];
        int[] counts = new int[inputs.size()];
        for (int i = 0; i < inputs.size(); i++) {
            counts[i] = (int) Math.min(NetworkItemCache.getCount(recipeId + "|" + i), Integer.MAX_VALUE);
        }
        return counts;
    }
}
