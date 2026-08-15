# Dummy

语言 / Language: 简体中文 | [English](README.en_US.md)

![:Dummy](https://count.getloli.com/@railgun19457_Dummy?name=railgun19457_Dummy&theme=minecraft&padding=6&offset=0&align=top&scale=1&pixelated=1&darkmode=auto)

Dummy 是一个面向高版本 Paper 服务端的假人插件，通过服务端真实假玩家实体提供背包、装备、经验、动作、皮肤、持久化和区块加载能力。

## 功能特性

- 基于 Paper + NMS 创建真实 `ServerPlayer` 假人
- 支持召唤、移除、列表、重载和传送管理
- 支持右键假人打开 GUI，管理背包、装备栏和副手栏
- 支持假人配置：无敌、碰撞、幽灵模式、区块加载、Tab 显示和名字格式
- 支持默认继承召唤者皮肤，并可按正版玩家名拉取皮肤
- 支持皮肤缓存，减少重复请求 Mojang API
- 支持假人背包、装备、副手、经验、皮肤、配置和位置持久化
- 支持移除后保留假人数据，同名重新召唤可恢复已保存资料
- 支持按世界 `simulation-distance` 加载假人周围区块，用于红石和实体挂机场景
- 支持 `zh_CN` 和 `en_US` 双语消息
- 支持 `/dummy` 和 `/dm` 命令别名

## 支持列表

| 项目 | 支持范围 |
| --- | --- |
| Java | 21+ |
| 服务端 | Paper |
| 已验证 Paper 版本 | 1.21.11、26.1.2、26.2 |

## 安装

1. 从 Release 下载插件 Jar（或本地构建）。
2. 放入 Paper 服务端的 `plugins` 目录。
3. 启动服务端，首次启动会自动生成配置文件：
   - `plugins/Dummy/config.yml`
   - `plugins/Dummy/lang/zh_CN.yml`
   - `plugins/Dummy/lang/en_US.yml`
   - `plugins/Dummy/dummies.yml`
   - `plugins/Dummy/skins.yml`

## 命令

| 命令 | 说明 | 权限节点 |
| --- | --- | --- |
| `/dummy` | 显示插件帮助信息 | `dummy.command` |
| `/dm` | `/dummy` 的别名 | `dummy.command` |
| `/dummy spawn <name>` | 在当前位置召唤假人 | `dummy.command.spawn` |
| `/dummy remove <name>` | 移除指定假人 | `dummy.command.remove` |
| `/dummy remove all` | 移除自己可管理的全部假人 | `dummy.command.remove` |
| `/dummy delete <name>` | 移除假人并删除保存数据 | `dummy.command.delete` |
| `/dummy list` | 查看当前假人列表 | `dummy.command.list` |
| `/dummy reload` | 重载配置和语言文件 | `dummy.command.reload` |
| `/dummy config <name> [key] [value]` | 打开配置 GUI 或修改假人配置 | `dummy.command.config` |
| `/dummy skin <name> set <playerName>` | 使用指定正版玩家皮肤 | `dummy.command.skin` |
| `/dummy skin <name> clear` | 清除假人皮肤 | `dummy.command.skin` |
| `/dummy exp <name> <amount\|all>` |将假人经验转移给执行命令的玩家 | `dummy.command.exp` |
| `/dummy inv <name>` | 打开假人背包、装备栏和副手栏 | `dummy.command.inv` |
| `/dummy tpto <name>` | 将玩家传送到假人处 | `dummy.command.tpto` |
| `/dummy tphere <name>` | 将假人传送到玩家当前位置 | `dummy.command.tphere` |
| `/dummy tps <name>` | 交换玩家和假人的位置 | `dummy.command.tps` |
| `/dummy actions <name>` | 查看假人当前正在执行的重复动作 | `dummy.command.actions` |
| `/dummy actions <name> <action> ...` | 控制假人执行动作 | `dummy.command.actions` |

## 动作

可用动作：

- `attack` 攻击当前锁定目标；没有锁定目标时攻击视线指向的可见实体，并受距离、冷却和视线遮挡限制；可用 `actions.attack.auto-target-nearest-visible` 开启最近可见实体 fallback
- `chat <message>` 发送聊天消息
- `command <command>` 以假人身份执行命令
- `drop` 丢弃当前物品
- `hold <0-8>` 切换快捷栏槽位
- `jump` 跳跃
- `look direction <east|west|north|south>` 朝向固定方向
- `look entity [player [玩家名]|monster]` 看向实体目标；不指定类型时默认最近可见实体，`player` 不指定玩家名时默认最近可见玩家，`monster` 为最近可见敌对生物
- `look angle <yaw|~> <pitch|~>` 转向指定角度，`~` 表示保持当前 yaw 或 pitch，重复执行实体追踪时会平滑转向并忽略被方块遮挡的目标
- `lookat <x> <y> <z>` 看向指定方块坐标，支持 `~`、`~1`、`~-1`
- `mine` 挖掘视线内方块
- `mount` 骑乘或离开附近可骑乘实体
- `move [speed|slow|walk|sprint]` 朝当前视线方向移动，速度范围为 `0` 到正常疾跑速度，默认正常行走速度，速度/缓慢药水会额外叠加
- `place` 使用主手方块对视线内目标方块进行放置
- `sneak [toggle|on|off]` 切换潜行状态
- `swap` 交换主手和副手物品
- `use` 使用主手物品
- `stop [action]` 停止全部动作或指定动作

### 动作示例

```text
/dummy actions bot attack
/dummy actions bot attack repeat interval:20
/dummy actions bot attack repeat interval:20 duration:1200
/dummy actions bot look entity player repeat interval:1
/dummy actions bot lookat ~ ~ ~5
/dummy actions bot move repeat
/dummy actions bot move sprint repeat duration:100
/dummy actions bot jump repeat
/dummy actions bot
/dummy actions bot stop
/dummy actions bot stop attack
```

动作模式说明：

- 未指定模式时执行一次。
- `repeat interval:<ticks> duration:<ticks>` 表示重复执行，`interval:` 和 `duration:` 均可省略。
- 省略 `interval:` 时使用动作默认周期，例如 `move repeat` 为持续行走，`jump repeat` 为正常连续跳跃。
- `jump` 的循环周期最短为一次完整跳跃所需的正常周期。
- 重复动作默认会在假人死亡、移除、退出时暂停，并在同 UUID 假人重新出现后继续，可通过 `actions.preserve-on-lifecycle` 关闭。

## 权限

| 权限节点 | 说明 | 默认 |
| --- | --- | --- |
| `dummy.command` | 允许使用基础命令 | 所有玩家 |
| `dummy.command.manage-all` | 允许管理其他玩家召唤的假人 | OP |
| `dummy.command.spawn` | 允许召唤假人 | 所有玩家 |
| `dummy.command.remove` | 允许移除单个假人 | 所有玩家 |
| `dummy.command.delete` | 允许移除假人并删除保存数据 | 所有玩家 |
| `dummy.command.list` | 允许查看假人列表 | 所有玩家 |
| `dummy.command.reload` | 允许重载配置 | OP |
| `dummy.command.config` | 允许修改假人配置 | 所有玩家 |
| `dummy.command.skin` | 允许修改假人皮肤 | 所有玩家 |
| `dummy.command.exp` | 允许转移假人经验 | 所有玩家 |
| `dummy.command.inv` | 允许打开假人背包 | 所有玩家 |
| `dummy.command.tpto` | 允许传送到假人 | 所有玩家 |
| `dummy.command.tphere` | 允许将假人传送到当前位置 | 所有玩家 |
| `dummy.command.tps` | 允许交换位置 | 所有玩家 |
| `dummy.command.actions` | 允许控制假人动作 | 所有玩家 |

普通玩家默认只能在列表、补全、配置、背包、传送、经验、动作、移除和删除命令中操作自己召唤的假人。拥有 `dummy.command.manage-all` 的玩家可操作全部假人；`/dummy remove all` 会移除当前玩家可管理的全部假人。

## 配置概览

`config.yml` 主要分区：

- `language`：语言文件，内置 `zh_CN`、`en_US`
- `defaults`：新建假人的默认配置
- `limits`：全服假人数量上限和单玩家假人数量上限，`-1` 表示无限制
- `storage`：启动恢复和移除后保留数据
- `inventory`：假人移除或插件关闭时是否掉落背包、装备和副手物品
- `commands`：假人创建前后由控制台执行的命令
- `death`：假人死亡后是否自动重新召唤
- `actions`：动作系统配置，例如生命周期保留动作、攻击 fallback 和骑乘搜索范围

可通过命令修改的假人配置项：

- `invulnerable`：是否无敌
- `collision`：是否启用碰撞
- `ghost`：幽灵模式，假人会无敌、无碰撞、无重力且不加载区块
- `chunk-loader`：是否按世界 `simulation-distance` 加载周围区块
- `show-in-tab`：是否显示在 Tab 列表
- `name-format`：假人显示名格式，支持 `%name%`

创建前后命令支持的占位符：

- `%dummy%`：假人名称
- `%uuid%`：假人 UUID
- `%creator%`：创建者名称
- `%creator_uuid%`：创建者 UUID
- `%world%`：召唤世界
- `%x%`：召唤 X 坐标
- `%y%`：召唤 Y 坐标
- `%z%`：召唤 Z 坐标

## 数据文件

- `dummies.yml`：保存当前假人和已移除假人的数据
- `skins.yml`：保存通过玩家名拉取到的皮肤缓存
- `lang/zh_CN.yml`：中文消息文件
- `lang/en_US.yml`：英文消息文件

## 本地构建

```bash
gradle clean build
```

构建产物位于：

- `build/libs/Dummy-<version>.jar`
