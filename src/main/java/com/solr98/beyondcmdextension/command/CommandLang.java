package com.solr98.beyondcmdextension.command;

import net.minecraft.network.chat.Component;
import com.solr98.beyondcmdextension.CommandConfig;

import java.util.HashMap;
import java.util.Map;

public class CommandLang
{
    private static final Map<String, Map<CommandConfig.Language, String>> TRANSLATIONS = new HashMap<>();

    static
    {
        register("network.list.title",
                CommandConfig.Language.EN_US, "=== Dimension Networks ===",
                CommandConfig.Language.ZH_CN, "=== 维度网络列表 ===");

        register("network.list.all_title",
                CommandConfig.Language.EN_US, "=== All Dimension Networks ===",
                CommandConfig.Language.ZH_CN, "=== 所有维度网络 ===");

        register("network.list.owner",
                CommandConfig.Language.EN_US, "Owner",
                CommandConfig.Language.ZH_CN, "所有者");

        register("network.list.players",
                CommandConfig.Language.EN_US, "Players",
                CommandConfig.Language.ZH_CN, "玩家数");

        register("network.list.managers",
                CommandConfig.Language.EN_US, "Managers",
                CommandConfig.Language.ZH_CN, "管理员数");

        register("network.list.deleted_mark",
                CommandConfig.Language.EN_US, "[DELETED]",
                CommandConfig.Language.ZH_CN, "[已删除]");

        register("network.list.none",
                CommandConfig.Language.EN_US, "No networks found.",
                CommandConfig.Language.ZH_CN, "未找到网络。");

        register("network.list.none_all",
                CommandConfig.Language.EN_US, "No networks found.",
                CommandConfig.Language.ZH_CN, "未找到网络。");

        register("network.info.title",
                CommandConfig.Language.EN_US, "=== Network Info (ID: %s) ===",
                CommandConfig.Language.ZH_CN, "=== 网络信息 (ID: %s) ===");

        register("network.info.unknown",
                CommandConfig.Language.EN_US, "Unknown",
                CommandConfig.Language.ZH_CN, "未知");

        register("network.info.status.active",
                CommandConfig.Language.EN_US, "Active",
                CommandConfig.Language.ZH_CN, "活跃");

        register("network.info.status.deleted",
                CommandConfig.Language.EN_US, "Deleted",
                CommandConfig.Language.ZH_CN, "已删除");

        register("network.info.players",
                CommandConfig.Language.EN_US, "Players",
                CommandConfig.Language.ZH_CN, "玩家数");

        register("network.info.managers",
                CommandConfig.Language.EN_US, "Managers",
                CommandConfig.Language.ZH_CN, "管理员数");

        register("network.info.slot_capacity",
                CommandConfig.Language.EN_US, "Slot Capacity (per type)",
                CommandConfig.Language.ZH_CN, "槽位容量 (每种)");

        register("network.info.slot_max_size",
                CommandConfig.Language.EN_US, "Slot Max Size (types)",
                CommandConfig.Language.ZH_CN, "槽位数量 (种类)");



        register("network.info.not_exist",
                CommandConfig.Language.EN_US, "Network does not exist: netId=%s",
                CommandConfig.Language.ZH_CN, "网络不存在：netId=%s");

        register("network.generateItems.success",
                CommandConfig.Language.EN_US, "Generated %s types of items, total %s items inserted into network %s",
                CommandConfig.Language.ZH_CN, "已生成 %s 种物品，共 %s 个物品插入到网络 %s");



        register("network.batchCreate.success",
                CommandConfig.Language.EN_US, "Successfully created %s networks",
                CommandConfig.Language.ZH_CN, "成功创建 %s 个网络");

        register("network.restore.success",
                CommandConfig.Language.EN_US, "Restored network: netId=%s",
                CommandConfig.Language.ZH_CN, "已恢复网络：netId=%s");

        register("network.restore.not_deleted",
                CommandConfig.Language.EN_US, "Network is not deleted: netId=%s",
                CommandConfig.Language.ZH_CN, "网络未被删除：netId=%s");

        register("network.restore.not_exist",
                CommandConfig.Language.EN_US, "Network does not exist: netId=%s",
                CommandConfig.Language.ZH_CN, "网络不存在：netId=%s");

        register("network.insert.success",
                CommandConfig.Language.EN_US, "Inserted %s x %s into network %s",
                CommandConfig.Language.ZH_CN, "已插入 %s x %s 到网络 %s");

        register("network.giveTerminal.success",
                CommandConfig.Language.EN_US, "Given portable network terminal (bound to netId=%s, owner=%s) x%s",
                CommandConfig.Language.ZH_CN, "已给予便携网络终端 (绑定到 netId=%s, 所有者=%s) x%s");

        register("network.giveTerminal.not_exist",
                CommandConfig.Language.EN_US, "Network does not exist: netId=%s",
                CommandConfig.Language.ZH_CN, "网络不存在：netId=%s");

        register("error.server_not_available",
                CommandConfig.Language.EN_US, "Server not available.",
                CommandConfig.Language.ZH_CN, "服务器不可用。");

        register("error.player_required",
                CommandConfig.Language.EN_US, "This command must be run by a player.",
                CommandConfig.Language.ZH_CN, "此命令必须由玩家执行。");

        register("error.item_not_found",
                CommandConfig.Language.EN_US, "Portable network terminal item not found.",
                CommandConfig.Language.ZH_CN, "未找到便携网络终端物品。");



        register("network.list.page",
                CommandConfig.Language.EN_US, "Page %s",
                CommandConfig.Language.ZH_CN, "第 %s 页");

        register("network.list.previous",
                CommandConfig.Language.EN_US, "◀ Previous",
                CommandConfig.Language.ZH_CN, "◀ 上一页");

        register("network.list.next",
                CommandConfig.Language.EN_US, "Next ▶",
                CommandConfig.Language.ZH_CN, "下一页 ▶");

        register("network.list.page_with_total",
                CommandConfig.Language.EN_US, "Page %s/%s (Total: %s networks)",
                CommandConfig.Language.ZH_CN, "第 %s/%s 页 (总计: %s 个网络)");

        register("network.open.success",
                CommandConfig.Language.EN_US, "Opened %s GUI for network %s for player %s",
                CommandConfig.Language.ZH_CN, "已为玩家 %s 打开网络 %s 的 %s 界面");

        register("network.open.error.no_permission",
                CommandConfig.Language.EN_US, "Player %s does not have permission to access network %s",
                CommandConfig.Language.ZH_CN, "玩家 %s 没有权限访问网络 %s");

        register("network.open.error.not_exist",
                CommandConfig.Language.EN_US, "Network ID %s does not exist or has been deleted",
                CommandConfig.Language.ZH_CN, "网络ID %s 不存在或已被删除");

        register("network.open.error.player_required",
                CommandConfig.Language.EN_US, "This command must be executed by a player or specify a target player",
                CommandConfig.Language.ZH_CN, "此命令必须由玩家执行或指定目标玩家");

        register("network.open.error.general",
                CommandConfig.Language.EN_US, "Error opening GUI: %s",
                CommandConfig.Language.ZH_CN, "打开界面时出错: %s");

        register("error.not_in_network",
                CommandConfig.Language.EN_US, "You are not in any network.",
                CommandConfig.Language.ZH_CN, "您不在任何网络中。");

        register("error.network_not_found",
                CommandConfig.Language.EN_US, "ID does not correspond to any network (or it was deleted).",
                CommandConfig.Language.ZH_CN, "ID不对应任何网络（或已被删除）。");

        register("network.restore.note",
                CommandConfig.Language.EN_US, "Note: Network ID restoration may be limited in this version",
                CommandConfig.Language.ZH_CN, "注意：此版本中网络ID恢复可能受限");

        register("error.network_id_limit",
                CommandConfig.Language.EN_US, "Cannot find available network ID (may have reached the limit of 10000)",
                CommandConfig.Language.ZH_CN, "无法找到可用的网络ID（可能已达到上限10000）");

        register("network.open.menu.storage",
                CommandConfig.Language.EN_US, "Network Storage",
                CommandConfig.Language.ZH_CN, "网络存储");

        register("network.open.menu.crafting",
                CommandConfig.Language.EN_US, "Network Crafting",
                CommandConfig.Language.ZH_CN, "网络合成");

        register("network.open.menu.terminal",
                CommandConfig.Language.EN_US, "Network Terminal",
                CommandConfig.Language.ZH_CN, "网络终端");

        register("network.batchAdd.success",
                CommandConfig.Language.EN_US, "Successfully added %s players as %s to network %s: %s",
                CommandConfig.Language.ZH_CN, "成功添加 %s 个玩家为网络 %s 的%s: %s");

        register("network.batchAdd.already_in_network",
                CommandConfig.Language.EN_US, "Players already in network: %s",
                CommandConfig.Language.ZH_CN, "以下玩家已在网络中: %s");

        register("network.batchAddPlayer.success",
                CommandConfig.Language.EN_US, "Successfully added player %s as %s to %s networks",
                CommandConfig.Language.ZH_CN, "成功添加玩家 %s 为%s到 %s 个网络");

        register("network.batchAddPlayer.already_in_networks",
                CommandConfig.Language.EN_US, "Player already in networks: %s",
                CommandConfig.Language.ZH_CN, "玩家已在以下网络中: %s");

        register("network.batchAddPlayer.not_exist",
                CommandConfig.Language.EN_US, "Networks do not exist: %s",
                CommandConfig.Language.ZH_CN, "以下网络不存在: %s");

        register("network.batchAddPlayer.no_permission",
                CommandConfig.Language.EN_US, "No permission to add player to networks: %s",
                CommandConfig.Language.ZH_CN, "无权限添加玩家到以下网络: %s");

        register("network.batchAddPlayer.failed",
                CommandConfig.Language.EN_US, "Failed to add player to networks: %s",
                CommandConfig.Language.ZH_CN, "添加玩家到以下网络失败: %s");

        register("network.batchAddPlayer.error.invalid_networks",
                CommandConfig.Language.EN_US, "Invalid network IDs: %s",
                CommandConfig.Language.ZH_CN, "无效的网络ID: %s");

        register("network.batchAddPlayer.no_networks",
                CommandConfig.Language.EN_US, "No networks specified",
                CommandConfig.Language.ZH_CN, "未指定网络");

        register("network.batchAdd.failed",
                CommandConfig.Language.EN_US, "Failed to add players: %s",
                CommandConfig.Language.ZH_CN, "以下玩家添加失败: %s");

        register("network.batchAdd.no_players",
                CommandConfig.Language.EN_US, "No players were added (player list may be empty)",
                CommandConfig.Language.ZH_CN, "没有玩家被添加（玩家列表可能为空）");

        register("network.myNetworks.title.self",
                CommandConfig.Language.EN_US, "=== Your Network Permissions ===",
                CommandConfig.Language.ZH_CN, "=== 您拥有的网络权限 ===");

        register("network.myNetworks.title.other",
                CommandConfig.Language.EN_US, "=== Network Permissions for Player %s ===",
                CommandConfig.Language.ZH_CN, "=== 玩家 %s 拥有的网络权限 ===");

        register("network.myNetworks.none",
                CommandConfig.Language.EN_US, "This player has no network permissions",
                CommandConfig.Language.ZH_CN, "该玩家没有任何网络权限");

        register("network.myNetworks.permission.owner",
                CommandConfig.Language.EN_US, "Owner",
                CommandConfig.Language.ZH_CN, "所有者");

        register("network.myNetworks.permission.manager",
                CommandConfig.Language.EN_US, "Manager",
                CommandConfig.Language.ZH_CN, "管理员");

        register("network.myNetworks.permission.member",
                CommandConfig.Language.EN_US, "Member",
                CommandConfig.Language.ZH_CN, "成员");



        register("error.op_required_for_others",
                CommandConfig.Language.EN_US, "Only OP can view other players' network information",
                CommandConfig.Language.ZH_CN, "只有OP可以查看其他玩家的网络信息");

        register("network.batchAdd.error.owner_required",
                CommandConfig.Language.EN_US, "Only network owner can add managers to network %s",
                CommandConfig.Language.ZH_CN, "只有网络所有者可以向网络 %s 添加管理员");

        register("network.batchAdd.error.owner_or_manager_required",
                CommandConfig.Language.EN_US, "Only network owner or manager can add members to network %s",
                CommandConfig.Language.ZH_CN, "只有网络所有者或管理员可以向网络 %s 添加成员");

        register("error.invalid_resource_type",
                CommandConfig.Language.EN_US, "Invalid resource type: %s",
                CommandConfig.Language.ZH_CN, "无效的资源类型: %s");

        register("network.batchAddPlayer.error.no_valid_networks",
                CommandConfig.Language.EN_US, "No valid networks found to add player to",
                CommandConfig.Language.ZH_CN, "未找到有效的网络来添加玩家");

        register("network.batchAddPlayer.no_networks",
                CommandConfig.Language.EN_US, "No networks specified",
                CommandConfig.Language.ZH_CN, "未指定网络");

        register("network.batchRemove.failed",
                CommandConfig.Language.EN_US, "Failed to remove players: %s",
                CommandConfig.Language.ZH_CN, "以下玩家移除失败: %s");

        register("network.batchRemove.no_players",
                CommandConfig.Language.EN_US, "No players were removed (player list may be empty)",
                CommandConfig.Language.ZH_CN, "没有玩家被移除（玩家列表可能为空）");

        register("network.batchRemove.not_in_network",
                CommandConfig.Language.EN_US, "Players not in network: %s",
                CommandConfig.Language.ZH_CN, "以下玩家不在网络中: %s");

        register("network.batchRemove.success",
                CommandConfig.Language.EN_US, "Successfully removed %s players from network %s: %s",
                CommandConfig.Language.ZH_CN, "成功从网络 %s 移除 %s 个玩家: %s");

        register("network.generateResources.success",
                CommandConfig.Language.EN_US, "Generated %s resources of type %s, total %s inserted into network %s",
                CommandConfig.Language.ZH_CN, "已生成 %s 个%s资源，共 %s 插入到网络 %s");

        register("network.myNetworks.info.format",
                CommandConfig.Language.EN_US, "Network %s | Permission: %s | Owner: %s | Players: %s | Managers: %s",
                CommandConfig.Language.ZH_CN, "网络 %s | 权限: %s | 所有者: %s | 玩家: %s | 管理员: %s");

        register("network.info.title",
                CommandConfig.Language.EN_US, "=== Network Info (ID: %s) ===",
                CommandConfig.Language.ZH_CN, "=== 网络信息 (ID: %s) ===");

        register("network.info.owner_label",
                CommandConfig.Language.EN_US, "Owner: %s | Status: ",
                CommandConfig.Language.ZH_CN, "所有者: %s | 状态: ");

        register("network.info.status.active",
                CommandConfig.Language.EN_US, "Active",
                CommandConfig.Language.ZH_CN, "活跃");

        register("network.info.status.deleted",
                CommandConfig.Language.EN_US, "Deleted",
                CommandConfig.Language.ZH_CN, "已删除");

        register("network.info.crystal_time",
                CommandConfig.Language.EN_US, "Crystal Remaining Time: ",
                CommandConfig.Language.ZH_CN, "结晶生成剩余时间: ");

        register("network.info.slot_capacity_label",
                CommandConfig.Language.EN_US, "Slot Capacity: ",
                CommandConfig.Language.ZH_CN, "槽位容量: ");

        register("network.info.slot_count_label",
                CommandConfig.Language.EN_US, " | Slot Count: ",
                CommandConfig.Language.ZH_CN, " | 槽位数量: ");

        register("network.info.storage_stats",
                CommandConfig.Language.EN_US, "Storage Statistics:",
                CommandConfig.Language.ZH_CN, "存储统计:");

        register("network.info.items_label",
                CommandConfig.Language.EN_US, "  Items: ",
                CommandConfig.Language.ZH_CN, "  物品: ");

        register("network.info.types_suffix",
                CommandConfig.Language.EN_US, " types, Total: ",
                CommandConfig.Language.ZH_CN, " 种, 总量: ");

        register("network.info.fluids_label",
                CommandConfig.Language.EN_US, "  Fluids: ",
                CommandConfig.Language.ZH_CN, "  流体: ");

        register("network.info.energy_label",
                CommandConfig.Language.EN_US, "  Energy: ",
                CommandConfig.Language.ZH_CN, "  能量: ");

        register("network.info.player_count_label",
                CommandConfig.Language.EN_US, "Player Count: ",
                CommandConfig.Language.ZH_CN, "玩家数: ");

        register("network.info.manager_count_label",
                CommandConfig.Language.EN_US, " | Managers: ",
                CommandConfig.Language.ZH_CN, " | 管理员: ");

        register("network.info.player_list_label",
                CommandConfig.Language.EN_US, "Player List: ",
                CommandConfig.Language.ZH_CN, "玩家列表: ");

        register("network.info.no_players",
                CommandConfig.Language.EN_US, "No players",
                CommandConfig.Language.ZH_CN, "无玩家");
    }

    private static void register(String key, CommandConfig.Language lang1, String text1, CommandConfig.Language lang2, String text2)
    {
        TRANSLATIONS.computeIfAbsent(key, k -> new HashMap<>()).put(lang1, text1);
        TRANSLATIONS.computeIfAbsent(key, k -> new HashMap<>()).put(lang2, text2);
    }

    public static String get(String key)
    {
        CommandConfig.Language lang = CommandConfig.getCommandLanguage();
        Map<CommandConfig.Language, String> langMap = TRANSLATIONS.get(key);
        if (langMap == null)
        {
            return key;
        }
        String text = langMap.get(lang);
        return text != null ? text : langMap.get(CommandConfig.Language.EN_US);
    }

    public static String get(String key, Object... args)
    {
        return String.format(get(key), args);
    }

    public static Component component(String key)
    {
        return Component.literal(get(key));
    }

    public static Component component(String key, Object... args)
    {
        return Component.literal(get(key, args));
    }
}