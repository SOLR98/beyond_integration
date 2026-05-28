package com.solr98.beyondintegration.command.util;

import com.solr98.beyondintegration.command.CommandLang;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

public class PermissionChecker {

    public static boolean checkServerAvailable(CommandSourceStack s) {
        if (s.getServer() == null) {
            s.sendFailure(CommandLang.component("error.server_not_available"));
            return false;
        }
        return true;
    }

    public static ServerPlayer checkPlayer(CommandSourceStack source) {
        try {
            return source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(CommandLang.component("error.player_required"));
            return null;
        }
    }

    public static boolean checkOpPermission(CommandSourceStack source) {
        if (!CommandUtils.hasOpPermission(source)) {
            source.sendFailure(CommandLang.component("error.op_required"));
            return false;
        }
        return true;
    }

    public static boolean checkOpPermissionForOthers(CommandSourceStack source, ServerPlayer targetPlayer) {
        if (!CommandUtils.hasOpPermission(source) && !source.getPlayer().getUUID().equals(targetPlayer.getUUID())) {
            source.sendFailure(CommandLang.component("error.op_required_for_others"));
            return false;
        }
        return true;
    }

    public static DimensionsNet checkNetworkExists(CommandSourceStack source, int netId) {
        DimensionsNet net = DimensionsNet.getNetFromId(netId);
        if (net == null) {
            source.sendFailure(CommandLang.component("error.network_not_found"));
            return null;
        }
        return net;
    }

    public static boolean checkNetworkAccessPermission(CommandSourceStack source, DimensionsNet net, ServerPlayer player) {
        return CommandUtils.checkNetworkAccessPermission(source, net, player);
    }

    public static boolean checkNetworkManagementPermission(CommandSourceStack source, DimensionsNet net, ServerPlayer player) {
        return CommandUtils.checkNetworkManagementPermission(source, net, player);
    }

    public static boolean checkOwner(CommandSourceStack s, DimensionsNet net, ServerPlayer p) {
        if (net.isOwner(p.getUUID())) return true;
        s.sendFailure(CommandLang.component("network.batchAdd.error.owner_required", net.getId()));
        return false;
    }

    public static boolean checkOwnerOrManager(CommandSourceStack s, DimensionsNet net, ServerPlayer p) {
        UUID id = p.getUUID();
        if (net.isOwner(id) || net.isManager(id)) return true;
        s.sendFailure(CommandLang.component("network.batchAdd.error.owner_or_manager_required", net.getId()));
        return false;
    }

    public static boolean checkAddManagerPermission(CommandSourceStack source, DimensionsNet net, ServerPlayer player) {
        if (CommandUtils.hasOpPermission(source)) return true;
        if (!CommandUtils.isNetworkOwner(player, net)) {
            source.sendFailure(CommandLang.component("network.batchAdd.error.owner_required", net.getId()));
            return false;
        }
        return true;
    }

    public static boolean checkAddMemberPermission(CommandSourceStack source, DimensionsNet net, ServerPlayer player) {
        if (CommandUtils.hasOpPermission(source)) return true;
        if (!CommandUtils.hasNetworkManagementPermission(player, net)) {
            source.sendFailure(CommandLang.component("network.batchAdd.error.owner_or_manager_required", net.getId()));
            return false;
        }
        return true;
    }

    public static boolean checkRemoveMemberPermission(CommandSourceStack source, DimensionsNet net, ServerPlayer player) {
        return checkAddMemberPermission(source, net, player);
    }

    public static boolean checkNetworkIdValid(CommandSourceStack source, int netId) {
        if (!CommandUtils.isValidNetworkId(netId)) {
            source.sendFailure(Component.literal("Invalid network ID: " + netId));
            return false;
        }
        return true;
    }

    public static boolean checkAmountPositive(CommandSourceStack source, long amount) {
        if (amount <= 0) {
            source.sendFailure(CommandLang.component("error.amount_must_be_positive"));
            return false;
        }
        return true;
    }

    public static boolean checkPlayerListNotEmpty(CommandSourceStack source, List<ServerPlayer> players) {
        if (players == null || players.isEmpty()) {
            source.sendFailure(CommandLang.component("network.batchAdd.no_players"));
            return false;
        }
        return true;
    }

    public static boolean checkNetworkListNotEmpty(CommandSourceStack source, List<Integer> netIds) {
        if (netIds == null || netIds.isEmpty()) {
            source.sendFailure(CommandLang.component("network.batchAddPlayer.no_networks"));
            return false;
        }
        return true;
    }

    public static boolean checkResourceTypeValid(CommandSourceStack source, String resourceType) {
        String[] valid = {"items", "fluids", "energy", "mixed", "all"};
        for (String v : valid) if (v.equalsIgnoreCase(resourceType)) return true;
        source.sendFailure(CommandLang.component("error.invalid_resource_type", resourceType));
        return false;
    }
}
