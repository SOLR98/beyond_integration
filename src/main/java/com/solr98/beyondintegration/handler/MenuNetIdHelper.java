package com.solr98.beyondintegration.handler;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import java.lang.reflect.Field;

public class MenuNetIdHelper {

    private static final Field beyond$STORAGE_FIELD;
    private static final Field beyond$NET_FIELD;

    static {
        Field sf = null, nf = null;
        try {
            sf = Class.forName("com.wintercogs.beyonddimensions.common.menu.DimensionsNetMenu").getField("storage");
            nf = UnifiedStorage.class.getDeclaredField("net");
            nf.setAccessible(true);
        } catch (Exception ignored) {}
        beyond$STORAGE_FIELD = sf;
        beyond$NET_FIELD = nf;
    }

    /** 客户端：从当前打开的 BD 菜单中读取同步过来的网络 ID */
    public static int getNetIdFromMenu(Player player) {
        if (player.containerMenu instanceof NetIdAccessor acc)
            return acc.beyond$getNetId();
        return -1;
    }

    /**
     * 服务端：从当前打开的 BD 菜单中反射获取 DimensionsNet
     * 利用 UnifiedStorage 内部持有的 net 字段
     */
    @Nullable
    public static DimensionsNet getNetFromMenu(ServerPlayer player) {
        var menu = player.containerMenu;
        if (menu == null || beyond$STORAGE_FIELD == null || beyond$NET_FIELD == null) return null;
        try {
            Object storage = beyond$STORAGE_FIELD.get(menu);
            if (storage == null) return null;
            Object net = beyond$NET_FIELD.get(storage);
            return net instanceof DimensionsNet dn ? dn : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
