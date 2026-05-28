package com.solr98.beyondintegration.command;

import com.solr98.beyondintegration.CommandConfig;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;

public class CommandLang {
    private static final Map<String, Map<CommandConfig.Language, String>> T = new HashMap<>();

    static {
        reg("network.list.title", "====== Dimension Networks ======", "========= 维度网络列表 =========");
        reg("network.list.all_title", "====== All Dimension Networks ======", "========= 所有维度网络 =========");
        reg("network.list.owner", "Owner", "所有者");
        reg("network.list.players", "Players", "玩家数");
        reg("network.list.managers", "Managers", "管理员数");
        reg("network.list.deleted_mark", "[DELETED]", "[已删除]");
        reg("network.list.none", "No networks found.", "未找到网络。");
        reg("network.list.permission", "Permission", "权限");
        reg("network.list.name", "Name", "名称");
        reg("network.list.name.none", "-", "无");
        reg("network.list.page", "Page %s", "第 %s 页");
        reg("network.list.previous", "<- Prev", "<- 上页");
        reg("network.list.next", "Next ->", "下页 ->");
        reg("network.list.page_with_total", "Page %s/%s (T:%s)", "第 %s/%s 页 (共%s)");
        reg("network.list.player_title", "======= %s's Networks ", "========== %s 的网络 ");

        reg("network.info.title", "======= Network Info (ID: %s) =======", "======= 网络信息 (ID: %s) =======");
        reg("network.info.name_label", "Name: ", "名称: ");
        reg("network.info.owner_label", "Owner: %s | Status: ", "所有者: %s | 状态: ");
        reg("network.info.your_permission_label", " | Your Permission: ", " | 你的权限: ");
        reg("network.info.status.active", "Active", "活跃");
        reg("network.info.status.deleted", "Deleted", "已删除");
        reg("network.info.crystal_time", "Crystal Time: ", "结晶时间: ");
        reg("network.info.slot_capacity_label", "Slot Capacity: ", "槽位容量: ");
        reg("network.info.slot_count_label", " | Slot Count: ", " | 槽位数量: ");
        reg("network.info.storage_stats", "Storage Statistics:", "存储统计:");
        reg("network.info.items_label", "  Items: ", "  物品: ");
        reg("network.info.fluids_label", "  Fluids: ", "  流体: ");
        reg("network.info.energy_label", "  Energy: ", "  能量: ");
        reg("network.info.types_suffix", " types, Total: ", " 种, 总量: ");
        reg("network.info.player_count_label", "Player Count: ", "玩家数: ");
        reg("network.info.manager_count_label", " | Managers: ", " | 管理员: ");
        reg("network.info.player_list_label", "Player List: ", "玩家列表: ");
        reg("network.info.no_players", "No players", "无玩家");
        reg("network.info.no_resources", "  No resources", "  无资源");
        reg("network.info.no_permission", "No Permission", "无权限");
        reg("network.info.unknown", "Unknown", "未知");

        reg("network.myNetworks.title.self", " Your Network Permissions", " 您拥有的网络权限 ");
        reg("network.myNetworks.title.other", "=== Network Permissions for Player %s ===", "======= 玩家 %s 拥有的网络权限 =======");
        reg("network.myNetworks.none", "This player has no network permissions", "该玩家没有任何网络权限");
        reg("network.myNetworks.permission.owner", "Owner", "所有者");
        reg("network.myNetworks.permission.manager", "Manager", "管理员");
        reg("network.myNetworks.permission.member", "Member", "成员");
        reg("network.myNetworks.info.format", "Network %s | %s | Owner: %s | P: %s | M: %s", "网络 %s | %s | 所有者: %s | 玩家: %s | 管理员: %s");

        reg("network.open.success", "Opened %s GUI for network %s for player %s", "已为玩家 %3$s 打开网络 %2$s 的 %1$s 界面");
        reg("network.open.error.no_permission", "Player %s does not have permission to access network %s", "玩家 %s 没有权限访问网络 %s");
        reg("network.open.error.not_exist", "Network ID %s does not exist or has been deleted", "网络ID %s 不存在或已被删除");
        reg("network.open.error.player_required", "This command must be executed by a player or specify a target player", "此命令必须由玩家执行或指定目标玩家");
        reg("network.open.error.general", "Error opening GUI: %s", "打开界面时出错: %s");
        reg("network.open.error.failed", "Failed to open interface", "打开界面失败");
        reg("network.open.error.no_permission_control", "Player %s does not have permission to control network %s (requires owner or manager)", "玩家 %s 没有权限控制网络 %s (需要所有者或管理员权限)");
        reg("network.open.error.no_primary_network", "Player %s does not have a primary network", "玩家 %s 没有主要网络");
        reg("network.open.error.cannot_get_network_id", "Cannot get network ID", "无法获取网络ID");
        reg("network.open.menu.storage", "Network Storage", "网络存储");
        reg("network.open.menu.crafting", "Network Crafting", "网络合成");
        reg("network.open.menu.terminal", "Network Terminal", "网络终端");
        reg("network.open.menu.permission", "Network Permission Control", "网络权限控制");

        reg("network.insert.item.success", "Inserted %s x %s into network %s", "已插入 %s x %s 到网络 %s");
        reg("network.insert.fluid.success", "Inserted %s mB of %s into network %s", "已插入 %s mB %s 到网络 %s");
        reg("network.insert.energy.success", "Inserted %s FE energy into network %s", "已插入 %s FE 能量到网络 %s");

        reg("network.giveTerminal.success", "Given portable network terminal (bound to netId=%s, owner=%s) x%s", "已给予便携网络终端 (绑定到 netId=%s, 所有者=%s) x%s");
        reg("network.giveTerminal.item_name", "Owner: %s's Network #%s", "所有者: %s 的 %s号网络");
        reg("network.giveTerminal.item_description", "Owner: %s\nNetwork ID: %s", "所有者: %s\n网络ID: %s");
        reg("network.giveTerminal.not_exist", "Network does not exist: netId=%s", "网络不存在：netId=%s");

        reg("network.tools.giveEnchantedBooks.success", "Gave %s %s enchanted books", "已给予 %s %s 本附魔书");
        reg("network.tools.giveEnchantedBooks.all_info", "Creating enchanted book with all %s enchantments for %s...", "正在为 %2$s 创建包含所有 %1$s 个附魔的附魔书...");

        reg("network.batchAdd.success", "Successfully added %s players as %s to network %s: %s", "成功添加 %s 个玩家为网络 %3$s 的%s: %4$s");
        reg("network.batchAdd.no_players", "No players were added (player list may be empty)", "没有玩家被添加（玩家列表可能为空）");
        reg("network.batchAdd.failed", "Failed to add players: %s", "以下玩家添加失败: %s");
        reg("network.batchAdd.already_in_network", "Players already in network: %s", "以下玩家已在网络中: %s");
        reg("network.batchAdd.error.owner_required", "Only network owner can add managers to network %s", "只有网络所有者可以向网络 %s 添加管理员");
        reg("network.batchAdd.error.owner_or_manager_required", "Only network owner or manager can add members to network %s", "只有网络所有者或管理员可以向网络 %s 添加成员");

        reg("network.batchAddPlayer.success", "Successfully added %s/%s players as %s to network %s", "成功添加 %s/%s 名玩家为%s到网络 %s");
        reg("network.batchAddPlayer.no_networks", "No networks specified", "未指定网络");
        reg("network.batchAddPlayer.failed", "Failed to add player to network(s): %s (netId=%s)", "添加玩家到以下网络失败: %s (netId=%s)");
        reg("network.batchAddPlayer.not_exist", "Network(s) do not exist: %s", "以下网络不存在: %s");
        reg("network.batchAddPlayer.no_permission", "No permission to add player to network(s): %s", "无权限添加玩家到以下网络: %s");
        reg("network.batchAddPlayer.already_in_networks", "Player already in network(s): %s", "玩家已在以下网络中: %s");
        reg("network.batchAddPlayer.error.invalid_networks", "Invalid network IDs: %s", "无效的网络ID: %s");

        reg("network.batchAddToNetworks.success", "Successfully added %s/%s players as %s", "成功添加 %s/%s 名玩家为%s");
        reg("network.batchAddToNetworks.already_in_network", "Players already in network(s):", "玩家已在以下网络中:");
        reg("network.batchAddToNetworks.no_permission", "No permission to add players to network(s):", "无权限添加玩家到以下网络:");
        reg("network.batchAddToNetworks.failed", "Failed to add players to network(s):", "添加玩家到以下网络失败:");

        reg("network.batchRemove.success", "Successfully removed %s players from network %s", "成功从网络 %s 移除 %s 个玩家");
        reg("network.batchRemove.no_players", "No players were removed (player list may be empty)", "没有玩家被移除（玩家列表可能为空）");
        reg("network.batchRemove.failed", "Failed to remove players: %s (netId=%s)", "以下玩家移除失败: %s (netId=%s)");
        reg("network.batchRemove.not_in_network", "Players not in network: %s", "以下玩家不在网络中: %s");

        reg("network.batchCreate.success", "Successfully created %s networks", "成功创建 %s 个网络");
        reg("network.batchCreate.error.no_available_id", "Cannot find available network ID", "无法找到可用网络ID");
        reg("network.batchCreate.error.creation_failed", "Network #%s: Creation failed (player may already have a network)", "网络#%s: 创建失败（玩家可能已有网络）");
        reg("network.batchCreate.error.general_failure", "Network #%s: %s", "网络#%s: %s");
        reg("network.batchCreate.error.no_networks_created", "No networks were created", "未创建任何网络");

        reg("network.generate.result.title", "Network %s resource generation result (%s):", "网络 %s 资源生成结果 (%s):");
        reg("network.generate.result.items", "  Items: %s types, %s total", "  物品: %s 种, 总量: %s");
        reg("network.generate.result.fluids", "  Fluids: %s types, %s mB total", "  流体: %s 种, 总量: %s mB");
        reg("network.generate.result.energy", "  Energy: %s types, %s FE total", "  能量: %s 种, 总量: %s FE");
        reg("network.generate.result.enchantments", "  Enchantments: %s items with %s enchantments total", "  附魔: %s 个物品带有 %s 个附魔");
        reg("network.generate.result.nbt_size", "  NBT Data: %s items with NBT, estimated size: %s", "  NBT数据: %s 个物品带有NBT, 估算大小: %s");
        reg("network.generate.result.nbt_warning", "  Warning: Complex NBT may affect performance and storage", "  警告: 复杂NBT可能影响性能和存储");
        reg("network.generateResources.detailed_total", "  Total: %s resource types, %s resources inserted", "  总计: %s 种资源类型, %s 个资源已插入");
        reg("network.generateItems.resource_type.items", "item", "物品");
        reg("network.generateItems.resource_type.fluids", "fluid", "流体");
        reg("network.generateItems.resource_type.energy", "energy", "能量");
        reg("network.generateItems.resource_type.mixed", "mixed", "混合");
        reg("network.generateItems.resource_type.all", "all", "全部");

        reg("network.transfer.insufficient_storage", "Insufficient storage space in target network", "目标网络存储空间不足");
        reg("network.transfer.permission_denied", "You don't have permission to transfer from network %s", "你没有权限从网络 %s 传输物品");

        reg("pagination.click_to_page", "Click to go to page %s", "点击前往第 %s 页");

        reg("error.server_not_available", "Server not available.", "服务器不可用。");
        reg("error.player_required", "This command must be run by a player.", "此命令必须由玩家执行。");
        reg("error.not_in_network", "You are not in any network.", "您不在任何网络中。");
        reg("error.network_not_found", "ID does not correspond to any network.", "ID不对应任何网络。");
        reg("error.feature_removed", "%s has been removed.", "%s 已移除。");
        reg("error.op_required", "OP permission required.", "需要OP权限。");
        reg("error.op_required_for_others", "Only OP can view other players' network information", "只有OP可以查看其他玩家的网络信息");
        reg("error.item_not_found", "Portable network terminal item not found.", "未找到便携网络终端物品。");
        reg("error.amount_must_be_positive", "Amount must be greater than 0", "数量必须大于0");
        reg("error.players_required", "Players argument is required", "需要指定玩家参数");
        reg("error.item_required", "Item argument is required", "需要指定物品参数");
        reg("error.fluid_required", "Fluid argument is required", "需要指定流体参数");
        reg("error.insert_failed", "Insert failed: %s items remaining", "插入失败: 剩余 %s 个");
        reg("error.invalid_resource_type", "Invalid resource type: %s", "无效的资源类型: %s");
        reg("error.add_player_failed", "Failed to add player: %s", "添加玩家失败: %s");
        reg("error.remove_player_failed", "Failed to remove player: %s", "移除玩家失败: %s");
        reg("error.enchantment_book_not_implemented", "Enchantment book giving functionality is not yet implemented", "给予附魔书功能暂未实现");
        reg("error.network_id_limit", "Cannot find available network ID (may have reached the limit of 10000)", "无法找到可用的网络ID（可能已达到上限10000）");

        reg("display.empty_item", "Empty", "空");
        reg("display.empty_fluid", "Empty Fluid", "空流体");
        reg("display.disabled", "Disabled", "已禁用");
        reg("display.total_items", "Total items: %s", "物品总数: %s");
        reg("display.fluid_total", "Fluid total: %s mB", "流体总量: %s mB");
        reg("display.energy_total", "Energy total: %s FE", "能量总量: %s FE");
        reg("display.crystal_remaining_time", "Crystal Remaining: %s", "结晶剩余: %s");
        reg("display.crystal_generation_disabled", "Crystal generation is disabled", "结晶生成功能已禁用");
        reg("display.crystal_time_tooltip", "Time until next crystal generation", "距离下一次结晶生成的时间");
        reg("display.resource_type_count", "%s count: %s", "%s数量: %s");

        reg("button.accept", "[Accept]", "[接受]");
        reg("button.deny", "[Deny]", "[拒绝]");
        reg("button.cancel", "[Cancel]", "[取消]");
        reg("button.hover.accept", "Click to accept", "点击接受");
        reg("button.hover.deny", "Click to deny", "点击拒绝");
        reg("button.hover.cancel", "Click to cancel", "点击取消");

        reg("time.format.days_hours_minutes_seconds", "%sd %sh %sm %ss", "%s天 %s时 %s分 %s秒");
        reg("time.format.hours_minutes_seconds", "%sh %sm %ss", "%s时 %s分 %s秒");
        reg("time.format.minutes_seconds", "%sm %ss", "%s分 %s秒");
        reg("time.format.seconds", "%ss", "%s秒");
        reg("time.format.less_than_second", "<1s", "<1秒");
    }

    private static void reg(String k, String en, String zh) {
        var m = new HashMap<CommandConfig.Language, String>();
        m.put(CommandConfig.Language.EN_US, en);
        m.put(CommandConfig.Language.ZH_CN, zh);
        T.put(k, m);
    }

    public static String get(String key, Object... args) {
        var lang = CommandConfig.getCommandLanguage();
        var m = T.get(key);
        if (m == null) return key;
        var t = m.get(lang);
        if (t == null) t = m.get(CommandConfig.Language.EN_US);
        return t == null ? key : String.format(t, args);
    }

    public static Component component(String key, Object... args) {
        return Component.literal(get(key, args));
    }
}
