# 命令链路文档

## 命令树结构

```
/bdtools
├── network
│   ├── list [player] [page]
│   ├── info [netId] [player]
│   ├── insert item <item> [count] [netId]
│   │         fluid <fluid> [amount] [netId]
│   │         energy [amount] [netId]
│   ├── generateResources [netId] [typeCount] [minAmount] [maxAmount] {items|fluids|energy|mixed|all}
│   ├── giveTerminal [netId] [count]
│   ├── giveEnchantedBooks <player> [count] {random|all}
│   └── batchCreate <player> [count] [slotCapacity] [slotMaxSize]
├── member
│   ├── addMembers <players> [to <netId1..5>]
│   ├── addManagers <players> [to <netId1..5>]
│   ├── removePlayers <netId> <players>
│   └── removeManagers <netId> <players>
├── myNetworks [list|info|netId]
├── open [netId] [terminal|permission|control|craft]
├── openAny [netId] [terminal|permission|control|craft]   (OP only)
├── enchant separate
└── transfer                                                    (已移除占位)
```

## 统一错误消息

| 场景 | EN | ZH |
|------|----|-----|
| 服务器不可用 | Server not available. | 服务器不可用。 |
| 非玩家执行 | This command must be run by a player. | 此命令必须由玩家执行。 |
| 不在网络中 | You are not in any network. | 您不在任何网络中。 |
| 网络不存在 | ID does not correspond to any network. | ID不对应任何网络。 |
| 需要OP权限 | OP permission required. | 需要OP权限。 |
| 查看他人需OP | Only OP can view other players' network information | 只有OP可以查看其他玩家的网络信息 |

---

## `/bdtools network list`

列出网络，OP专用，支持分页和按玩家过滤。

| 场景 | 消息 |
|------|------|
| **成功** | `====== Dimension Networks ======` (或 `====== %s's Networks `) + 表头 `ID \| Owner \| Players \| Managers \| Permission` + 数据行 + 分页导航 |
| 无网络 | No networks found. / 未找到网络。 |
| 错误 | Server not available. / Player required. |

---

## `/bdtools network info`

查看网络详细信息。

| 场景 | 消息 |
|------|------|
| **成功** | `======= Network Info (ID: %s) =======` + `Owner: %s \| Status: Active/Deleted \| Your Permission: Owner/Manager/Member` + `Crystal Time: %s` + `Slot Capacity: %s \| Slot Count: %s` + `Storage Statistics:` + `Items: %s types, Total: %s` + `Fluids: %s types, Total: %s mB` + `Energy: %s types, Total: %s FE` + `Player Count: %s \| Managers: %s` + `Player List: [Owner] [Manager] [Member]` |
| 错误 | Server not available. / Not in network. / ID does not correspond to any network. |

---

## `/bdtools network insert`

向网络插入资源，需OP权限。

### item
| 场景 | 消息 |
|------|------|
| 成功 | `Inserted %s x %s into network %s` / `已插入 %s x %s 到网络 %s` |
| 需指定物品 | Item argument is required / 需要指定物品参数 |
| 存储满 | Insufficient storage space in target network / 目标网络存储空间不足 |
| 插入失败 | Insert failed: %s items remaining / 插入失败: 剩余 %s 个 |

### fluid
| 场景 | 消息 |
|------|------|
| 成功 | `Inserted %s mB of %s into network %s` / `已插入 %s mB %s 到网络 %s` |
| 需指定流体 | Fluid argument is required / 需要指定流体参数 |
| 存储满 | Insufficient storage space in target network |

### energy
| 场景 | 消息 |
|------|------|
| 成功 | `Inserted %s FE energy into network %s` / `已插入 %s FE 能量到网络 %s` |
| 存储满 | Insufficient storage space in target network |

---

## `/bdtools network generateResources`

生成测试资源，OP专用。

| 场景 | 消息 |
|------|------|
| **成功** | `Network %s resource generation result (%s):` + `Items: %s types, %s total` + `Fluids: %s types, %s mB total` + `Energy: %s types, %s FE total` + `Enchantments: %s items with %s enchantments total` + `NBT Data: %s items with NBT, estimated size: %s` + `Total: %s resource types, %s resources inserted` |
| 错误 | Server not available. / Not in network. |

---

## `/bdtools network giveTerminal`

给予便携网络终端，OP专用。

| 场景 | 消息 |
|------|------|
| 成功 | `Given portable network terminal (bound to netId=%s, owner=%s) x%s` / `已给予便携网络终端 (绑定到 netId=%s, 所有者=%s) x%s` |
| 物品未找到 | Portable network terminal item not found. / 未找到便携网络终端物品。 |

---

## `/bdtools network giveEnchantedBooks`

给予附魔书，OP专用。

| 场景 | 消息 |
|------|------|
| 成功 | `Gave %s %s enchanted books` / `已给予 %s %s 本附魔书` |
| all模式提示 | `Creating enchanted book with all %s enchantments for %s...` / `正在为 %2$s 创建包含所有 %1$s 个附魔的附魔书...` |
| 功能未实现 | Enchantment book giving functionality is not yet implemented / 给予附魔书功能暂未实现 |

---

## `/bdtools network batchCreate`

批量创建网络，OP专用。

| 场景 | 消息 |
|------|------|
| 成功 | `Successfully created %s networks for %s` / `成功创建 %s 个网络` + 逐项失败原因 |
| 创建失败 | `Network #%s: Creation failed (player may already have a network)` / `网络#%s: 创建失败（玩家可能已有网络）` |
| 无网络创建 | No networks were created / 未创建任何网络 |

---

## `/bdtools member addMembers / addManagers`

批量添加成员/管理员到网络。

### 添加默认网络（不指定to）
| 场景 | 消息 |
|------|------|
| 成功 | `Successfully added %s/%s players as Owner/Manager/Member to network %s (name1, name2)` / `成功添加 %s/%s 名玩家为Owner/Manager/Member到网络 %s` |
| 全部失败 | `Failed to add player to network(s): Owner/Manager/Member` / `添加玩家到以下网络失败: Owner/Manager/Member` |

### 添加指定网络（to netId1..5）
| 场景 | 消息 |
|------|------|
| 成功 | `Successfully added %s/%s players as Owner/Manager/Member (name1, name2)` / `成功添加 %s/%s 名玩家为Owner/Manager/Member` |
| 全部失败 | `Failed to add players: Owner/Manager/Member` / `以下玩家添加失败: Owner/Manager/Member` |

### 通用
| 场景 | 消息 |
|------|------|
| 无玩家 | No players were added (player list may be empty) / 没有玩家被添加（玩家列表可能为空） |
| 无权限 | Only network owner can add managers / 只有网络所有者可以向网络 %s 添加管理员 |
| 权限不够 | Only network owner or manager can add members / 只有网络所有者或管理员可以向网络 %s 添加成员 |
| 添加失败 | Failed to add player: %s / 添加玩家失败: %s |

---

## `/bdtools member removePlayers / removeManagers`

批量移除成员/管理员。

| 场景 | 消息 |
|------|------|
| 成功 | `Successfully removed %s players from network %s (name1, name2)` / `成功从网络 %s 移除 %s 个玩家` |
| 全部失败 | `Failed to remove players: Owner/Manager/Member` / `以下玩家移除失败: Owner/Manager/Member` |
| 缺少参数 | Players argument is required / 需要指定玩家参数 |
| 不能移除所有者 | `Cannot remove owner` (translatable) |
| 移除失败 | Failed to remove player: %s / 移除玩家失败: %s |

---

## `/bdtools myNetworks`

查看自己的网络权限。

| 场景 | 消息 |
|------|------|
| **成功(list)** | `Your Network Permissions` + 表头 `ID \| Permission \| Owner \| Players \| Managers` + 数据行 |
| 无网络 | This player has no network permissions / 该玩家没有任何网络权限 |

---

## `/bdtools open / openAny`

打开网络GUI。

| 场景 | 消息 |
|------|------|
| 成功 | `Opened Network Storage/Crafting/Terminal/Permission Control GUI for network %s for player %s` / `已为玩家 %s 打开网络 %s 的 Network Storage/Crafting/Terminal/Permission Control 界面` |
| 无权访问 | Player %s does not have permission to access network %s / 玩家 %s 没有权限访问网络 %s |
| 控制权限不足 | Player %s does not have permission to control network %s (requires owner or manager) / 玩家 %s 没有权限控制网络 %s (需要所有者或管理员权限) |
| 打开失败 | Error opening GUI: %s / 打开界面时出错: %s |

---

## `/bdtools enchant separate`

分离网络中的附魔书。

| 场景 | 消息 |
|------|------|
| 成功 | `EnchantmentBookSeparatorHandler.separateAll()` 返回的分离结果 |
| 无主网络 | `message.beyond_cmd_extension.no_primary_network` (translatable) |
| 执行失败 | `message.beyond_cmd_extension.execute_failed: %s` (translatable) |

---

## `/bdtools transfer`

已移除，仅占位。

| 场景 | 消息 |
|------|------|
| 始终 | `transfer has been removed.` / `网络传输命令 已移除。` |

## 辅助提示文本

| 用途 | EN | ZH |
|------|----|-----|
| 分页提示 | Click to go to page %s | 点击前往第 %s 页 |
| 接受按钮 | [Accept] | [接受] |
| 拒绝按钮 | [Deny] | [拒绝] |
| 取消按钮 | [Cancel] | [取消] |
| 悬浮提示(接受) | Click to accept | 点击接受 |
| 悬浮提示(拒绝) | Click to deny | 点击拒绝 |
| 悬浮提示(取消) | Click to cancel | 点击取消 |
| 悬停物品数 | Total items: %s | 物品总数: %s |
| 悬停能量 | Energy total: %s FE | 能量总量: %s FE |
| 结晶剩余 | Crystal Remaining: %s | 结晶剩余: %s |
| 结晶禁用 | Crystal generation is disabled | 结晶生成功能已禁用 |
