package com.solr98.beyondintegration.command.util;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.solr98.beyondintegration.command.CommandLang;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.util.PlayerNameHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class CommandUtils {

    public static final int OP_LEVEL = 2;

    public static ServerPlayer getExecutorPlayer(CommandSourceStack source) throws CommandSyntaxException {
        return source.getPlayerOrException();
    }

    public static boolean hasOpPermission(CommandSourceStack source) {
        return source.hasPermission(OP_LEVEL);
    }

    public static boolean isPlayerInNetwork(ServerPlayer player, DimensionsNet net) {
        return net != null && net.getPlayers().contains(player.getUUID());
    }

    public static boolean isNetworkOwner(ServerPlayer player, DimensionsNet net) {
        return net != null && net.isOwner(player.getUUID());
    }

    public static boolean isNetworkManager(ServerPlayer player, DimensionsNet net) {
        return net != null && net.isManager(player.getUUID());
    }

    public static boolean hasNetworkManagementPermission(ServerPlayer player, DimensionsNet net) {
        return isNetworkOwner(player, net) || isNetworkManager(player, net);
    }

    public static DimensionsNet getNetOrFail(CommandSourceStack source, int netId) {
        DimensionsNet net = DimensionsNet.getNetFromId(netId);
        if (net == null) {
            source.sendFailure(CommandLang.component("error.network_not_found"));
            return null;
        }
        return net;
    }

    public static boolean checkNetworkAccessPermission(CommandSourceStack source, DimensionsNet net, ServerPlayer player) {
        if (hasOpPermission(source)) return true;
        if (!isPlayerInNetwork(player, net)) {
            source.sendFailure(CommandLang.component("network.open.error.no_permission", player.getGameProfile().getName(), net.getId()));
            return false;
        }
        return true;
    }

    public static boolean checkNetworkManagementPermission(CommandSourceStack source, DimensionsNet net, ServerPlayer player) {
        if (hasOpPermission(source)) return true;
        if (!hasNetworkManagementPermission(player, net)) {
            source.sendFailure(CommandLang.component("network.open.error.no_permission_control", player.getGameProfile().getName(), net.getId()));
            return false;
        }
        return true;
    }

    public static String getPlayerNameByUUID(UUID playerUuid, MinecraftServer server) {
        if (server != null) {
            return PlayerNameHelper.getPlayerNameByUUID(playerUuid, server);
        }
        return getPlayerNameFromServer(playerUuid, server);
    }

    private static String getPlayerNameFromServer(UUID uuid, MinecraftServer server) {
        if (server == null) return CommandLang.get("network.info.unknown");
        var player = server.getPlayerList().getPlayer(uuid);
        if (player != null) return player.getName().getString();
        var profile = server.getProfileCache().get(uuid);
        return profile.map(p -> p.getName()).orElse(CommandLang.get("network.info.unknown"));
    }

    public static String getNetworkOwnerName(DimensionsNet net, MinecraftServer server) {
        if (net == null || net.getOwner() == null) return CommandLang.get("network.info.unknown");
        return getPlayerNameFromServer(net.getOwner(), server);
    }

    public static String formatBigNumber(BigInteger number) {
        if (number.compareTo(BigInteger.valueOf(1_000_000_000_000_000_000L)) >= 0)
            return number.divide(BigInteger.valueOf(1_000_000_000_000_000_000L)) + "E";
        if (number.compareTo(BigInteger.valueOf(1_000_000_000_000_000L)) >= 0)
            return number.divide(BigInteger.valueOf(1_000_000_000_000_000L)) + "P";
        if (number.compareTo(BigInteger.valueOf(1_000_000_000_000L)) >= 0)
            return number.divide(BigInteger.valueOf(1_000_000_000_000L)) + "T";
        if (number.compareTo(BigInteger.valueOf(1_000_000_000L)) >= 0)
            return number.divide(BigInteger.valueOf(1_000_000_000L)) + "G";
        if (number.compareTo(BigInteger.valueOf(1_000_000L)) >= 0)
            return number.divide(BigInteger.valueOf(1_000_000L)) + "M";
        if (number.compareTo(BigInteger.valueOf(1_000L)) >= 0)
            return number.divide(BigInteger.valueOf(1_000L)) + "K";
        return number.toString();
    }

    public static String formatLongNumber(long number) {
        return formatBigNumber(BigInteger.valueOf(number));
    }

    public static String getItemName(ItemStack stack) {
        if (stack.isEmpty()) return CommandLang.get("display.empty_item");
        var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null ? id.toString() : stack.getDisplayName().getString();
    }

    public static boolean isValidNetworkId(int netId) {
        return netId >= 0 && netId < 10000;
    }

    public static List<Integer> getValidNetworkIds(List<Integer> netIds) {
        List<Integer> valid = new ArrayList<>();
        for (int id : netIds) if (isValidNetworkId(id)) valid.add(id);
        return valid;
    }

    public static boolean isValidPlayerName(String name) {
        return Pattern.compile("^[a-zA-Z0-9_]{3,16}$").matcher(name).matches();
    }
}
