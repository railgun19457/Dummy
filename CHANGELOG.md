# 更新日志

## 0.4.2 - 2026-08-16

### 修复

- 修复假人无法乘坐矿车、船等非生物载具的问题。
- 修复假人无法将矿车和船作为 `look` / `attack` 目标的问题。
- 修复插件重载或重启后重复动作丢失的问题。
- 修复插件重载或重启后假人乘坐状态丢失的问题；载具恢复会按 UUID 最多重试 100 tick。
- 修复 `/dm remove` 移除正在乘坐载具的假人时，载具被一并删除的问题。

### 持久化与兼容性

- 重复动作的命令参数（动作、参数、间隔和持续时间）会随假人数据保存，并在启动时自动恢复。
- 保存假人当前载具的 UUID，并在载具重新加载后恢复乘客关系。
- `DummyStorage` 自动迁移已有 SQLite 数据库，为旧数据添加 `vehicle_uuid` 和 `active_actions` 列，无需手动执行迁移脚本。
- 载具在销毁假人前会被标记为持久实体并解除乘客关系，避免 `PlayerList.remove` / `discard` 的清理流程级联删除矿车或船。

## 0.4.1 - 2026-08-16

### 修复

- 修复玩家上线后假人间歇性不可见的问题。

### 变更

- 皮肤切换改用 Paper `PlayerProfile` API（`Player.setPlayerProfile`），由 Paper 原子广播 `ClientboundPlayerInfoUpdatePacket`，不再手动重建实体追踪。
- 玩家上线时只同步 PlayerInfo 的 listed 标志（`updateListed`），实体显示交由 vanilla `ChunkMap` 自动处理。
- 移除手动实体追踪状态机及其兼容性 fallback，简化假人的上线和换肤同步流程。

## 0.4.0 - 2026-08-15

### 新增

- 持久化后端从 `dummies.yml` 切换到 SQLite，数据库位于 `plugins/Dummy/dummies.db`。
- SQLite 启用 WAL 模式；`ItemStack[]` 使用 Bukkit 序列化后保存为 BLOB，以兼容跨版本数据。
- 首次启动时自动将旧 `dummies.yml` 迁移到 SQLite，并将原文件重命名为 `dummies.yml.bak`。
- 引入 dirty 标记和周期性异步保存，默认每 600 tick（30 秒）保存一次。

### 优化

- 使用单行 upsert 替代全量重写，减少高频写盘造成的主线程卡顿。
- `mine repeat`、`place repeat` 和 `move repeat` 不再在每个 tick 阻塞主线程写盘。
- 动作和 GUI 操作改为标记数据变更，由统一的异步保存流程处理。
- `DummyManager.shutdown` 和 `/dummy reload` 会先同步 `flushNow`，确保关机和重载前数据落盘。

### 修复

- 修复假人被 `/kick` 或 NMS 强制断开时未按 `storage.keep-removed-data` 保存数据，导致同名假人重新召唤后背包、装备和经验无法恢复的问题。

## 0.3.1 - 2026-06-05

### 修复

- 修复 AstrBotAdapter 代理模式误选 Dummy 假人为 `astrbot:proxy` 插件消息载体时，后端认证、心跳和上报信道可能中断的问题。
- Dummy 假连接现在会将 `astrbot:proxy` 插件消息转交给真实在线玩家连接，避免假连接吞掉代理通信数据。

## 0.3.0 - 2026-05-22

### 变更

- 重构假人皮肤的获取、缓存和持久化，保存 signed textures、模型部件及获取时间。
- 根据真实皮肤数据和玩家客户端的皮肤部件设置控制披风显示。
- 使用 Paper/NMS 实体追踪器刷新假人，修复首次生成或换肤后客户端不可见的问题。

## 0.2.9 - 2026-05-22

### 新增

- 新增 `/dummy delete <name>`，移除假人的同时删除同名保存数据。
- `/dummy actions <name>` 现在会显示假人当前正在执行的重复动作列表。

### 变更

- 移除 `once` 动作模式；未指定 `repeat` 时默认执行一次。

## 0.2.8 - 2026-05-21

### 新增

- 重组 `look` 动作为 `direction` / `entity` / `angle` 三类参数，支持固定方向、角度 `~` 和实体分类追踪。
- `look entity` 支持玩家、指定玩家、敌对生物和默认实体目标，并在重复执行时平滑转向。
- 新增视线遮挡检测和目标锁定；`attack` 会优先攻击锁定目标，并校验距离、冷却和视线。
- 新增 `actions.attack.auto-target-nearest-visible` 配置项，默认关闭 `attack` 自动选择最近可见实体的 fallback。

## 0.2.7 - 2026-05-20

### 变更

- 移除独立的 `sprint` 动作；`move` 现在通过 `slow` / `walk` / `sprint` 或数值参数控制移动速度，并限制在正常疾跑速度以内。
- 重构重复动作参数为 `repeat interval:<ticks> duration:<ticks>`；省略时使用动作默认周期，`jump` 循环限制为完整跳跃周期。
- 优化假人管理权限：普通玩家只能管理自己召唤的假人，`dummy.command.manage-all` 可管理全部假人。
- `/dummy remove all` 改为移除当前玩家可管理的全部假人，不再使用独立的 `dummy.command.remove-all` 权限节点。

### 修复

- 修复切换 Tab 显示、换肤和重新进入服务器时假人可能不可见的问题，改用 packet-only 的 PlayerInfo、listed 和实体同步状态机。
- 延后玩家加入后的假人同步，减少客户端登录阶段丢弃实体包导致首次进服不可见的问题。

## 0.2.6 - 2026-05-19

### 变更

- 移除 `/dummy revive` 原地复活逻辑；假人死亡后改为按配置在重生点重新召唤。

### 修复

- 修复假人背包中使用 shift 快捷移动物品时可能无法正确移动的问题。
- 修复假人换肤后客户端可能需要重新进入服务器才显示新皮肤的问题。

## 0.2.5 - 2026-05-18

### 新增

- 新增 `actions.preserve-on-lifecycle` 配置项，默认启用。
- 默认保留假人死亡、移除和退出等生命周期中的重复动作，并在同 UUID 假人重新出现后继续执行。
- 新增 `place` 动作，假人可使用主手方块对视线内目标方块进行放置。
- `/dummy config <name>` 现在会直接打开与右键假人一致的配置 GUI。

### 变更

- `skin` 指令子命令调整为 `set` / `clear`；`set` 支持补全在线玩家名和不在线的正版玩家名。

## 0.2.4 - 2026-05-18

### 修复

- 修复假人死亡后仍保留在普通操作补全中，导致 `revive` 命令可能误报成功的问题。
- 修复关闭 Tab 显示的假人在玩家重新进入后可能不可见的问题。

### 变更

- 假人移除时会广播玩家离开提示。

## 0.2.3 - 2026-05-18

### 修复

- 修复假人拾取经验球时，带经验修补的装备可能无法正常修复的问题。
- 假人经验修补后会同步保存数据，避免重启或移除后装备耐久回退。

## 0.2.2 - 2026-05-18

### 新增

- 接入 ProxyTab 通用虚拟玩家协议 `proxytab:virtual_players`，让启用 Tab 显示的假人可由代理端统一格式化、排序和分组。

### 变更

- Tab 显隐改为使用 Paper `listPlayer` / `unlistPlayer` API，减少手动 NMS PlayerInfo 包带来的状态不一致。

### 修复

- 修复玩家重新进入服务器后，已关闭 Tab 显示的假人仍出现在 Tab 列表的问题。
- 修复假人刷新皮肤时可能重新强制显示在 Tab 列表的问题。

## 0.2.1 - 2026-05-15

### 新增

- 新增独立权限节点 `dummy.command.remove-all`，用于控制 `/dummy remove all`。

### 变更

- 调整默认权限：普通命令默认所有玩家可用，`reload` 和 `remove all` 仍默认 OP。
- 将中英文 README 的支持版本、命令和权限说明改为表格形式。

## 0.2.0 - 2026-05-15

### 兼容性

- 兼容 Paper 26.1.2 的 `ClientboundSetEntityMotionPacket` record accessor。
- 修复 Paper 26.1.2 下假人连接监听器未完全替换导致的重力异常。
- 修复 Paper 26.1.2 下假人受击后速度被服务端恢复覆盖，导致击退不生效的问题。

### 变更

- 改进假人移除流程，显式移除实体、Tab 列表项并关闭 fake connection。
- 为 fake channel / connection 增加关闭状态，避免移除后仍被视为连接中。
- 忽略 IDE 和 Java 输出目录 `bin/`。

## 0.1.0-SNAPSHOT - 2026-05-15

### 初始功能

- 实现基于 Paper + NMS `ServerPlayer` 的假人创建、移除、列表、复活和传送。
- 实现背包、装备、副手、经验、皮肤、动作、持久化、GUI 和区块加载。
- 提供 `zh_CN` / `en_US` 双语消息和中英文 README。
