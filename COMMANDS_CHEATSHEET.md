# Beyond Dimensions 命令速查表

**项目主页 → [README.md](README.md)** | **详细指南 → [命令简化指南](COMMANDS_SIMPLE.md)** | **完整文档 → [命令文档](COMMANDS.md)**

## 🎯 常用命令

### 查看网络
```bash
/bdtools myNetworks          # 我的主网络
/bdtools myNetworks list     # 我的所有网络
/bdtools network info        # 当前网络详情
```

### 打开界面
```bash
/bdtools open               # 存储
/bdtools open terminal      # 终端
/bdtools open craft         # 合成
/bdtools open permission    # 权限（需管理权限）
```

## 📋 命令分类

### 网络查看
| 命令 | 权限 | 说明 |
|------|------|------|
| `myNetworks` | 所有人 | 查看自己的网络 |
| `network info` | 网络成员 | 查看网络详情 |
| `network list` | 网络成员 | 列出网络 |

### 界面操作
| 命令 | 权限 | 说明 |
|------|------|------|
| `open` | 网络成员 | 存储界面 |
| `open terminal` | 网络成员 | 终端界面 |
| `open craft` | 网络成员 | 合成界面 |
| `open permission` | 网络管理员 | 权限管理 |

### 成员管理
| 命令 | 权限 | 说明 |
|------|------|------|
| `member addMembers` | 网络管理员 | 添加成员 |
| `member removePlayers` | 网络管理员 | 移除玩家 |
| `member addManagers` | 网络所有者 | 添加管理员 |
| `member removeManagers` | 网络所有者 | 移除管理员 |

### OP工具
| 命令 | 说明 |
|------|------|
| `network giveTerminal` | 给予网络终端 |
| `network giveEnchantedBooks` | 给予附魔书 |
| `network batchCreate` | 批量创建网络 |
| `network info nbt` | 计算NBT大小 |

## 🚀 快速示例

### 普通玩家
```bash
# 查看网络
/bdtools myNetworks
/bdtools open

# 使用网络
/bdtools open terminal
/bdtools open craft
```

### 网络管理员
```bash
# 管理成员
/bdtools member addMembers 123 Steve
/bdtools member removePlayers 123 Bob

# 权限管理
/bdtools open permission
```

### OP管理员
```bash
# 全局管理
/bdtools network list
/bdtools network info 123

# 特殊工具
/bdtools network giveTerminal 123
/bdtools network giveEnchantedBooks Steve 1 all
```

## ⭐ 新增功能

### 1. 超级附魔书
```bash
# 一本包含所有附魔的书
/bdtools network giveEnchantedBooks Steve 1 all
```

### 2. NBT分析
```bash
# 分析网络存储大小
/bdtools network info 123 nbt
```

### 3. 完整界面
- 存储界面
- 终端界面  
- 合成界面
- 权限控制界面

## 🔐 权限说明

**权限基于原版 Beyond Dimensions 系统**

### 基本规则
- 网络命令：需要网络访问权限
- 管理命令：需要网络管理权限
- OP命令：需要服务器OP权限

## ❓ 常见问题

### 命令无效？
1. 检查拼写
2. 检查权限
3. 查看错误提示

### 没有权限？
- 普通命令：需要是网络成员
- 管理命令：需要是网络管理员
- OP命令：需要服务器OP权限

### 如何获得权限？
- 网络成员：让管理员添加你
- 网络管理员：让所有者添加你
- OP：联系服务器管理员

## 📝 使用技巧

### 查看权限
```bash
/bdtools myNetworks list
```

### 测试命令
```bash
# 先试简单命令
/bdtools myNetworks
```

### 获取帮助
- 查看错误信息
- 尝试不同权限级别
- 参考此速查表

---

**版本**: 0.2.0  
**更新**: 附魔书all参数、NBT计算、完整界面  
**提示**: 从 `/bdtools myNetworks` 开始探索

## 相关文档
- 📖 **[命令简化指南](COMMANDS_SIMPLE.md)** - 详细说明和分类
- 📚 **[完整命令文档](COMMANDS.md)** - 全面功能介绍
- 👥 **[权限分类指南](COMMANDS_BY_PERMISSION.md)** - 按用户类型查看
- 🔧 **[权限配置说明](PERMISSIONS_AND_LIMITS.md)** - 高级权限设置