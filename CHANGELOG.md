# 更新日志

## 0.2.9 - 2026-05-22

- 移除 `once` 动作模式；未指定 `repeat` 时默认执行一次。
- 新增 `/dummy delete <name>`，移除假人的同时删除同名保存数据。
- `/dummy actions <name>` 现在会显示该假人当前正在执行的重复动作列表。

## 0.2.8 - 2026-05-21

- 重组 `look` 动作为 `direction` / `entity` / `angle` 三类参数，支持固定方向、角度 `~` 和实体分类追踪。
- `look entity` 支持玩家、指定玩家、敌对生物和默认实体目标，并在重复执行时平滑转向。
- 新增视线遮挡检测和目标锁定，`attack` 会优先攻击锁定目标，并校验距离、冷却和视线。
- 新增 `actions.attack.auto-target-nearest-visible` 配置项，默认关闭 `attack` 自动选择最近可见实体的 fallback。

## 0.2.7 - 2026-05-20

- 移除 `sprint` 动作，`move` 现在通过 `slow` / `walk` / `sprint` 或数值参数控制移动速度，并限制在正常疾跑速度以内。
- 重构重复动作参数为 `repeat interval:<ticks> duration:<ticks>`，省略时使用动作默认周期；`jump` 循环会限制到完整跳跃周期。
- 优化假人管理权限：普通玩家只能管理自己召唤的假人，`dummy.command.manage-all` 可管理全部假人。
- `/dummy remove all` 改为移除当前玩家可管理的全部假人，不再使用单独的 `dummy.command.remove-all` 权限节点。
- 修复切换 Tab 显示、换肤和重新进入服务器时假人可能不可见的问题，改为 packet-only 的 PlayerInfo/listed/实体同步状态机。
- 延后玩家加入后的假人同步，减少客户端登录阶段丢弃假人实体包导致首次进服不可见的问题。

## 0.2.6 - 2026-05-19

- 修复假人背包中 shift 快捷移动物品时可能无法正确移动的问题。
- 移除 `/dummy revive` 原地复活逻辑，假人死亡后改为按配置在重生点重新召唤。
- 修复假人换肤后客户端可能需要重新进入服务器才显示新皮肤的问题。

## 0.2.5 - 2026-05-18

- 新增 `actions.preserve-on-lifecycle` 配置项，默认启用。
- 默认保留假人死亡、移除、退出等生命周期中的重复动作，并在同 UUID 假人重新出现后继续执行。
- 新增 `place` 动作，假人可使用主手方块对视线内目标方块进行放置。
- `/dummy config <name>` 现在会直接打开与右键假人一致的配置 GUI。
- `skin` 指令子命令调整为 `set` / `clear`，`set` 补全在线玩家并支持不在线的正版玩家名。

## 0.2.4 - 2026-05-18

- 修复假人死亡后仍保留在普通操作补全中、`revive` 命令可能误报成功的问题。
- 修复关闭 Tab 显示的假人在玩家重新进入后可能不可见的问题。
- 假人移除时现在会广播玩家离开提示。

## 0.2.3 - 2026-05-18

- 修复假人拾取经验球时，带经验修补的装备可能无法正常修复的问题。
- 假人经验修补后会同步保存数据，避免重启或移除后装备耐久回退。

## 0.2.2 - 2026-05-18

- 修复玩家重新进入服务器后，已关闭 Tab 显示的假人仍出现在 Tab 列表的问题。
- Tab 显隐改为使用 Paper `listPlayer` / `unlistPlayer` API，减少手动 NMS PlayerInfo 包带来的状态不一致。
- 修复假人刷新皮肤时可能重新强制显示在 Tab 列表的问题。
- 接入 ProxyTab 通用虚拟玩家协议 `proxytab:virtual_players`，让启用 Tab 显示的假人可由代理端统一格式化、排序和分组。

## 0.2.1 - 2026-05-15

- 调整默认权限：普通命令默认所有玩家可用，`reload` 和 `remove all` 仍默认 OP。
- 新增独立权限节点 `dummy.command.remove-all`，用于控制 `/dummy remove all`。
- 将中英文 README 的支持版本、命令和权限说明改为表格形式。

## 0.2.0 - 2026-05-15

- 兼容 Paper 26.1.2 的 `ClientboundSetEntityMotionPacket` record accessor。
- 修复 26.1.2 下假人连接监听器未完全替换导致的重力异常。
- 修复 26.1.2 下假人受击后速度被服务端恢复覆盖，导致击退不生效的问题。
- 改进假人移除流程，显式移除实体、Tab 列表项并关闭 fake connection。
- 为 fake channel/connection 增加关闭状态，避免移除后仍被视为连接中。
- 忽略 IDE/Java 输出目录 `bin/`。

## 0.1.0-SNAPSHOT - 2026-05-15

- 初始快照版本。
- 实现基于 Paper + NMS `ServerPlayer` 的假人创建、移除、列表、复活和传送。
- 实现背包、装备、副手、经验、皮肤、动作、持久化、GUI 和区块加载。
- 提供 `zh_CN` / `en_US` 双语消息和中英文 README。
