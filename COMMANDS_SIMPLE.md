# Beyond Dimensions 命令简化指南

**项目主页 → [README.md](README.md)** | **快速查阅 → [命令速查表](COMMANDS_CHEATSHEET.md)** | **详细说明 → [完整文档](COMMANDS.md)**

## 快速开始

### 查看自己的网络
```bash
/bdtools myNetworks          # 查看主网络信息
/bdtools myNetworks list     # 列出所有网络
/bdtools network info        # 查看当前网络详情
```

### 打开网络界面
```bash
/bdtools open               # 存储界面
/bdtools open terminal      # 终端界面  
/bdtools open craft         # 合成界面
/bdtools open permission    # 权限控制（需要管理权限）
```

## 命令速查

### 网络查看命令
| 命令 | 谁可以用 | 功能 |
|------|----------|------|
| `/bdtools myNetworks` | 所有人 | 查看主网络 |
| `/bdtools myNetworks list` | 所有人 | 列出我的网络 |
| `/bdtools network info` | 网络成员 | 查看网络详情 |
| `/bdtools network info nbt` | OP | 计算NBT大小 |

### 界面命令
| 命令 | 谁可以用 | 功能 |
|------|----------|------|
| `/bdtools open` | 网络成员 | 打开存储界面 |
| `/bdtools open terminal` | 网络成员 | 打开终端 |
| `/bdtools open craft` | 网络成员 | 打开合成界面 |
| `/bdtools open permission` | 网络管理员 | 权限管理 |

### 成员管理
| 命令 | 谁可以用 | 功能 |
|------|----------|------|
| `/bdtools member addMembers` | 网络管理员 | 添加成员 |
| `/bdtools member addManagers` | 网络所有者 | 添加管理员 |
| `/bdtools member removePlayers` | 网络管理员 | 移除玩家 |
| `/bdtools member removeManagers` | 网络所有者 | 移除管理员 |

### OP专用工具
| 命令 | 功能 |
|------|------|
| `/bdtools network giveTerminal` | 给予网络终端 |
| `/bdtools network giveEnchantedBooks` | 给予附魔书 |
| `/bdtools network batchCreate` | 批量创建网络 |
| `/bdtools network insert` | 插入物品到网络 |
| `/bdtools network generateResources` | 生成测试资源 |

## 常用示例

### 普通玩家
```bash
# 查看自己的网络
/bdtools myNetworks
/bdtools myNetworks list

# 打开界面
/bdtools open
/bdtools open terminal
```

### 网络管理员
```bash
# 管理成员
/bdtools member addMembers 123 Steve Alex
/bdtools member removePlayers 123 Bob

# 权限管理
/bdtools open permission
```

### OP管理员
```bash
# 查看所有网络
/bdtools network list
/bdtools network info 123

# 工具命令
/bdtools network giveTerminal 123 3
/bdtools network giveEnchantedBooks Steve 1 all
```

## 新增功能

### 1. 附魔书"all"参数
```bash
# 给Steve一本包含所有附魔的书
/bdtools network giveEnchantedBooks Steve 1 all
```

### 2. NBT计算（OP专用）
```bash
# 计算网络123的NBT大小
/bdtools network info 123 nbt
```

### 3. 完整界面支持
```bash
# 打开各种界面
/bdtools open               # 存储
/bdtools open terminal      # 终端
/bdtools open craft         # 合成
/bdtools open permission    # 权限控制
```

## 权限说明

**注意**：权限系统基于原版 Beyond Dimensions 模组。我们的扩展只添加功能，不修改权限规则。

### 基本规则
- 查看网络：需要网络访问权限
- 管理成员：需要网络管理权限  
- OP命令：需要服务器OP权限（等级2）

## 常见问题

### Q: 命令不工作？
- 检查是否输入正确
- 检查是否有权限
- 查看错误信息提示

### Q: 如何成为网络成员？
- 让网络管理员添加你
- 或创建自己的网络

### Q: 如何获得OP权限？
- 联系服务器管理员
- 在控制台执行：`op <名字>`

## 版本信息
- **版本**: 0.2.0
- **新增**: 附魔书all参数、NBT计算、完整界面支持
- **改进**: 权限系统、错误处理、性能优化

---

**提示**: 使用 `/bdtools myNetworks` 查看自己有哪些网络权限。

## 相关文档
- 📋 **[命令速查表](COMMANDS_CHEATSHEET.md)** - 快速查阅常用命令
- 📖 **[完整命令文档](COMMANDS.md)** - 详细说明和示例
- 🔐 **[权限分类指南](COMMANDS_BY_PERMISSION.md)** - 按用户类型分类
- ⚙️ **[权限与限制说明](PERMISSIONS_AND_LIMITS.md)** - 详细权限配置