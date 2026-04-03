# Beyond Dimensions 命令扩展 - 详细使用文档

版本: 0.2.0 | 最后更新: 2026-04-04

## 目录
1. [命令概述](#命令概述)
2. [权限要求](#权限要求)
3. [主命令结构](#主命令结构)
4. [网络管理命令](#网络管理命令)
5. [成员管理命令](#成员管理命令)
6. [我的网络命令](#我的网络命令)
7. [打开界面命令](#打开界面命令)
8. [工具命令](#工具命令)
9. [高级功能](#高级功能)
10. [常见问题](#常见问题)

## 命令概述

Beyond Dimensions 命令扩展 (`bdtools`) 是一个功能强大的 Minecraft Forge 模组，为 Beyond Dimensions 模组提供了丰富的命令行管理工具。所有命令都需要 OP 权限（等级 2）。

## 权限说明

**重要**：所有权限规则基于原版 Beyond Dimensions 模组。本扩展只添加功能，不修改权限系统。

### 基本权限规则
- 网络相关命令：需要相应的网络访问权限
- OP专用命令：需要服务器OP权限（等级2）
- 具体权限由原版模组和服务器配置决定

## 主命令结构

```
/bdtools <子命令> [参数...]
```

主要子命令类别：
- `network` - 网络管理命令
- `member` - 成员管理命令
- `myNetworks` - 个人网络管理
- `open` - 打开网络界面

## 网络管理命令

### 1. 网络列表命令

**命令格式：**
```
/bdtools network list [player] [page]
```

**权限要求：**
- 查看所有网络：需要OP权限
- 查看特定玩家网络：需要OP权限或自己是该玩家
- 查看自己的网络：**普通玩家可用**

**参数说明：**
- `player` (可选) - 指定玩家名，查看该玩家的网络
- `page` (可选) - 页码，默认为 1

**功能：**
- 显示网络列表（分页显示）
- 支持按玩家筛选
- 显示网络ID、所有者、玩家数量、管理员数量
- 支持点击分页导航

**示例：**
```
# 普通玩家可以查看自己的网络
/bdtools network list                # 显示自己的网络列表

# OP可以查看所有网络
/bdtools network list 2              # 显示第2页所有网络
/bdtools network list Steve          # 显示Steve的网络
/bdtools network list Steve 2        # 显示Steve的网络第2页
```

### 2. 网络信息命令

**命令格式：**
```
/bdtools network info [netId] [player] [nbt]
```

**权限要求：**
- 查看自己的网络信息：**普通玩家可用**
- 查看其他玩家网络：需要OP权限
- NBT计算：需要OP权限 + 手动触发

**参数说明：**
- `netId` (可选) - 网络ID，默认为当前玩家的主要网络
- `player` (可选) - 指定玩家，查看该玩家视角的网络信息
- `nbt` (可选) - 手动触发NBT大小计算（仅OP可用）

**功能：**
- 显示网络详细信息
- 显示存储统计（物品类型、总数量、容量使用率）
- NBT 大小计算（按物品种类计算，手动触发）
- 使用 SI 单位格式化数字（K, M, G, T, P, E）
- 显示网络所有者、创建时间、成员数量

**示例：**
```
# 普通玩家可以查看自己的网络
/bdtools network info                # 显示当前玩家的主要网络信息

# OP可以查看任何网络
/bdtools network info 123            # 显示网络ID 123的信息
/bdtools network info 123 Alex       # 以Alex视角查看网络123
/bdtools network info 123 nbt        # 计算网络123的NBT大小（OP手动触发）
```

**NBT 计算说明：**
- 按物品种类计算，不按物品实例总数计算
- 相同NBT的物品只计算一次
- 需要OP权限手动触发
- 显示精确字节数和格式化大小

### 3. 网络插入命令

**命令格式：**
```
/bdtools network insert <netId> <item> <amount>
```

**参数说明：**
- `netId` (必需) - 网络ID
- `item` (必需) - 物品ID或物品名称
- `amount` (必需) - 插入数量

**功能：**
- 向指定网络插入物品
- 支持物品ID和物品名称

**示例：**
```
/bdtools network insert 123 minecraft:diamond 64
/bdtools network insert 123 diamond 64
```

### 4. 资源生成命令

**命令格式：**
```
/bdtools network generateResources <netId> <count>
```

**参数说明：**
- `netId` (必需) - 网络ID
- `count` (必需) - 生成数量（1-1000）

**功能：**
- 向指定网络生成随机资源
- 用于测试和开发
- 大多数物品会添加随机NBT数据

**示例：**
```
/bdtools network generateResources 123 100
```

## 成员管理命令

### 1. 添加成员命令

**命令格式：**
```
/bdtools member addMembers <netId> <players...>
```

**参数说明：**
- `netId` (必需) - 网络ID
- `players` (必需) - 一个或多个玩家名

**功能：**
- 向网络添加普通成员
- 支持批量添加多个玩家

**示例：**
```
/bdtools member addMembers 123 Steve Alex
```

### 2. 添加管理员命令

**命令格式：**
```
/bdtools member addManagers <netId> <players...>
```

**参数说明：**
- `netId` (必需) - 网络ID
- `players` (必需) - 一个或多个玩家名

**功能：**
- 向网络添加管理员
- 管理员拥有管理权限

**示例：**
```
/bdtools member addManagers 123 Steve
```

### 3. 移除玩家命令

**命令格式：**
```
/bdtools member removePlayers <netId> <players...>
```

**参数说明：**
- `netId` (必需) - 网络ID
- `players` (必需) - 一个或多个玩家名

**功能：**
- 从网络移除玩家（包括普通成员和管理员）
- 支持批量移除

**示例：**
```
/bdtools member removePlayers 123 Alex
```

### 4. 移除管理员命令

**命令格式：**
```
/bdtools member removeManagers <netId> <players...>
```

**参数说明：**
- `netId` (必需) - 网络ID
- `players` (必需) - 一个或多个玩家名

**功能：**
- 从网络移除管理员权限（降级为普通成员）
- 不会完全移除玩家

**示例：**
```
/bdtools member removeManagers 123 Steve
```

## 我的网络命令

### 1. 默认命令

**命令格式：**
```
/bdtools myNetworks
```

**权限要求：** **普通玩家可用**

**功能：**
- 显示当前玩家的主要网络信息
- 如果没有主要网络，显示网络列表
- **不支持查看其他玩家的网络**

**示例：**
```
/bdtools myNetworks
```

### 2. 列表命令

**命令格式：**
```
/bdtools myNetworks list [page]
```

**权限要求：** **普通玩家可用**

**参数说明：**
- `page` (可选) - 页码，默认为 1

**功能：**
- 显示当前玩家有权限的所有网络列表
- 分页显示，支持点击导航

**示例：**
```
/bdtools myNetworks list
/bdtools myNetworks list 2
```

### 3. 信息命令

**命令格式：**
```
/bdtools myNetworks info [netId]
```

**权限要求：** **普通玩家可用**（只能查看自己有权限的网络）

**参数说明：**
- `netId` (可选) - 网络ID，默认为主要网络

**功能：**
- 显示指定网络信息
- 功能与 `/bdtools network info` 相同
- 只能查看自己有访问权限的网络

**示例：**
```
/bdtools myNetworks info
/bdtools myNetworks info 123
```

## 打开界面命令

### 1. 默认打开命令

**命令格式：**
```
/bdtools open [netId] [interface]
```

**权限要求：**
- 打开自己的网络界面：**普通玩家可用**
- 打开权限控制界面：需要网络所有者或管理员权限
- 打开其他玩家网络：需要OP权限

**参数说明：**
- `netId` (可选) - 网络ID，默认为当前网络
- `interface` (可选) - 界面类型：`terminal`, `permission`, `control`, `craft`

**界面类型说明：**
- `terminal` - 网络终端界面
- `permission`/`control` - 权限控制界面（需要所有者或管理员权限）
- `craft` - 合成界面

**示例：**
```
# 普通玩家可以打开自己的网络界面
/bdtools open                    # 打开当前网络存储界面
/bdtools open terminal           # 打开当前网络终端

# 网络所有者/管理员可以打开权限控制
/bdtools open permission         # 打开当前网络权限控制界面
/bdtools open craft              # 打开当前网络合成界面

# OP可以打开任何网络
/bdtools open 123                # 打开网络123存储界面
/bdtools open 123 permission     # 打开网络123权限控制界面
/bdtools open 123 craft          # 打开网络123合成界面
```

### 2. 快捷命令

**命令格式：**
```
/bdtools open <interface>
```

**示例：**
```
/bdtools open terminal    # 打开当前网络终端
/bdtools open permission  # 打开当前网络权限控制界面
/bdtools open craft       # 打开当前网络合成界面
```

## 工具命令

### 1. 给予网络终端命令

**命令格式：**
```
/bdtools network giveTerminal [netId] [count]
```

**参数说明：**
- `netId` (可选) - 网络ID，默认为当前网络
- `count` (可选) - 数量，默认为 1

**功能：**
- 给予便携网络终端
- 终端绑定到指定网络（使用 `NetId` NBT字段）
- 显示网络所有者和网络ID

**示例：**
```
/bdtools network giveTerminal          # 给予当前网络终端 x1
/bdtools network giveTerminal 123      # 给予网络123终端 x1
/bdtools network giveTerminal 123 5    # 给予网络123终端 x5
```

### 2. 给予附魔书命令

**命令格式：**
```
/bdtools network giveEnchantedBooks <player> <count> [type] [minEnchants] [maxEnchants]
```

**参数说明：**
- `player` (必需) - 目标玩家
- `count` (必需) - 给予数量
- `type` (可选) - 附魔类型：`random`（默认）, `all`
- `minEnchants` (可选) - 最小附魔数量（仅限random类型）
- `maxEnchants` (可选) - 最大附魔数量（仅限random类型）

**附魔类型说明：**
- `random` - 随机附魔书（默认1-3个附魔）
- `all` - **一本包含所有附魔的书**（强制添加，忽略冲突）

**示例：**
```
# 随机附魔书
/bdtools network giveEnchantedBooks Steve 1
/bdtools network giveEnchantedBooks Steve 5 random
/bdtools network giveEnchantedBooks Steve 3 random 2 5

# 所有附魔书（新功能）
/bdtools network giveEnchantedBooks Steve 1 all
```

**特殊功能：**
- `all` 参数：给予一本包含**所有**附魔的书
- 强制添加所有附魔，忽略冲突和限制
- 使用最大等级

### 3. 批量创建网络命令

**命令格式：**
```
/bdtools network batchCreate <player> <count> [slotCapacity] [slotMaxSize]
```

**参数说明：**
- `player` (必需) - 网络所有者
- `count` (必需) - 创建数量（1-100）
- `slotCapacity` (可选) - 槽位容量，默认 1000000
- `slotMaxSize` (可选) - 最大槽位大小，默认 1000

**功能：**
- 批量创建网络
- 所有网络归指定玩家所有

**示例：**
```
/bdtools network batchCreate Steve 10
/bdtools network batchCreate Steve 5 500000 500
```

## 高级功能

### 1. NBT 大小计算

**功能特点：**
- 按物品种类计算，不按实例总数
- 相同NBT的物品只计算一次
- 使用SI单位格式化（KB, MB, GB等）
- OP手动触发，避免性能问题

**触发方式：**
```
/bdtools network info <netId> nbt
```

**输出信息：**
- 唯一带有NBT的物品类型数量
- 总NBT大小（字节）
- 格式化大小（KB/MB/GB）
- 大型数据警告（超过1GB）

### 2. 数字格式化

**SI单位系统：**
- K (kilo) - 千 (10³)
- M (mega) - 兆 (10⁶)
- G (giga) - 吉 (10⁹) - **使用G而不是B**
- T (tera) - 太 (10¹²)
- P (peta) - 拍 (10¹⁵)
- E (exa) - 艾 (10¹⁸)

**示例：**
- 1,000 → 1K
- 1,000,000 → 1M
- 1,000,000,000 → 1G
- 1,000,000,000,000 → 1T

### 3. 分页系统

**功能特点：**
- 所有列表命令支持分页
- 点击式导航（上一页/下一页）
- 显示总页数和总项目数
- 每页显示10个项目

**导航方式：**
```
[<上一页] [第1页/共5页 (50个项目)] [下一页>]
```
- 绿色文字表示可点击
- 鼠标悬停显示提示

## 常见问题

### Q1: 为什么需要OP权限？
A: 所有命令都需要OP等级2权限，这是服务器管理命令的标准安全要求。

### Q2: NBT计算为什么需要手动触发？
A: NBT计算可能消耗大量服务器资源，特别是对于大型网络。手动触发可以避免性能问题。

### Q3: 为什么`/bdtools myNetworks`不支持查看其他玩家？
A: 这是设计决策，`myNetworks`命令专门用于查看自己的网络。查看其他玩家网络请使用`/bdtools network list <player>`。

### Q4: 附魔书"all"参数有什么限制？
A: "all"参数会创建一本包含所有附魔的书，但某些附魔可能有冲突或限制。命令会强制添加所有附魔，但游戏本身可能限制某些组合。

### Q5: 如何查看网络存储的详细信息？
A: 使用`/bdtools network info <netId>`查看存储统计，或使用`/bdtools open <netId>`直接打开存储界面。

### Q6: 批量创建网络有什么用途？
A: 主要用于测试、开发或服务器设置。可以快速创建多个测试网络。

### Q7: 为什么使用SI单位而不是游戏惯例？
A: SI单位是国际标准，更符合技术文档的惯例。游戏中的"B"（billion）容易与字节单位混淆。

## 版本历史

### v0.2.0 (当前版本)
- 新增：附魔书"all"参数，给予一本包含所有附魔的书
- 修复：NBT计算逻辑，按物品种类计算而非实例总数
- 改进：使用SI单位格式化数字
- 修复：便携网络终端使用正确的`NetId`字段
- 新增：`/bdtools open`命令支持权限控制和合成界面
- 修复：`/bdtools myNetworks`默认显示主网络
- 改进：分页导航点击功能
- 新增：完整的命令文档

### v0.1.0
- 初始版本
- 基础网络管理命令
- 成员管理功能
- 基本工具命令

---

**注意：** 所有命令都需要在服务器控制台或拥有OP权限的玩家聊天框中输入。某些功能可能需要服务器重启后才能生效。

**技术支持：** 如有问题，请查看游戏日志或联系模组开发者。