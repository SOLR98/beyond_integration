# Beyond Integration 命令语言配置

## 配置文件位置

### 单人游戏/本地服务器
```
saves/<世界名>/serverconfig/beyond_integration-server.toml
```

### 专用服务器
```
serverconfig/beyond_integration-server.toml
```

## 配置项

### language.command_language
- **类型**: 枚举 (EN_US, ZH_CN)
- **默认值**: EN_US
- **说明**: 设置命令返回消息的语言

## 可用语言

| 代码 | 语言 |
|------|------|
| EN_US | 英语 (English) |
| ZH_CN | 简体中文 |

## 修改语言

### 方法 1: 编辑配置文件
1. 关闭服务器
2. 打开 `serverconfig/beyond_integration-server.toml`
3. 修改 `command_language` 为 `ZH_CN` 或 `EN_US`
4. 保存并重启服务器

### 方法 2: 游戏内配置
使用配置模组（如 Configured）在游戏内修改（需要管理员权限）

## 翻译文件位置
```
src/main/resources/assets/beyond_integration/lang/
├── en_us.json
└── zh_cn.json
```

## 添加新语言

1. 在 `lang` 文件夹创建新的语言文件（如 `ja_jp.json`）
2. 复制现有翻译文件内容
3. 翻译所有键值
4. 在 `CommandConfig.java` 的 `Language` 枚举中添加新语言
5. 在 `CommandLang.java` 中注册新语言的翻译

## 命令列表

| 命令 | 说明 |
|------|------|
| `/bdtools network list` | 列出所有活跃网络 |
| `/bdtools network list all` | 列出所有网络（包括已删除） |
| `/bdtools network info <netId>` | 查看指定网络详细信息 |
| `/bdtools network restore <netId>` | 恢复已删除网络（owner 设为 null） |
| `/bdtools network insert <netId> <item> [count]` | 向网络插入物品 |
| `/bdtools network giveTerminal <netId> [count]` | 获得绑定网络的便携终端 |
| `/bdtools network generateItems <netId> [typeCount] [minAmount] [maxAmount] [withEnchantments] [withNbt]` | 随机生成物品到网络 |
| `/bdtools network batchCreate <count> [slotCapacity] [slotMaxSize]` | 批量创建网络（owner 设为 null） |

## 翻译键列表

| 翻译键 | 说明 |
|--------|------|
| `network.list.title` | 网络列表标题 |
| `network.list.all_title` | 所有网络列表标题（含已删除） |
| `network.list.owner` | 所有者标签 |
| `network.list.players` | 玩家数标签 |
| `network.list.managers` | 管理员数标签 |
| `network.list.deleted_mark` | 已删除标记 |
| `network.list.none` | 无网络提示 |
| `network.list.none_all` | 无所有网络提示 |
| `network.info.title` | 网络信息标题 |
| `network.info.owner` | 所有者标签 |
| `network.info.unknown` | 未知所有者 |
| `network.info.status` | 状态标签 |
| `network.info.status.active` | 活跃状态 |
| `network.info.status.deleted` | 已删除状态 |
| `network.info.players` | 玩家数标签 |
| `network.info.managers` | 管理员数标签 |
| `network.info.slot_capacity` | 槽位容量标签（每种物品可存储数量） |
| `network.info.slot_max_size` | 槽位数量标签（可存储物品种类数） |
| `network.info.current_time` | 结晶生成时间标签 |
| `network.info.not_exist` | 网络不存在错误 |
| `network.generateItems.success` | 随机生成物品成功 |
| `network.generateItems.with_enchantments` | 附魔标签 |
| `network.generateItems.with_nbt` | 自定义 NBT 标签 |
| `network.batchCreate.success` | 批量创建网络成功 |
| `network.restore.success` | 恢复网络成功 |
| `network.restore.not_deleted` | 网络未删除错误 |
| `network.restore.not_exist` | 网络不存在错误 |
| `network.insert.success` | 插入物品成功 |
| `network.giveTerminal.success` | 给予终端成功 |
| `network.giveTerminal.not_exist` | 给予终端网络不存在错误 |
| `error.server_not_available` | 服务器不可用错误 |
| `error.player_required` | 需要玩家执行错误 |
| `error.item_not_found` | 物品未找到错误 |

## 注意事项

- 此为**服务器端配置**，每个世界可有独立的语言设置
- 客户端无需安装此模组即可看到翻译后的命令消息
- 配置更改后需要重启服务器或重新加载世界
