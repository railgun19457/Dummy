# Dummy

面向 Minecraft Java 1.20.x / 1.21.x 的假人插件工程，当前已完成基础工程与首批架构落地：

- Maven + Paper API 工程初始化
- 插件入口、`plugin.yml`、默认 `config.yml`
- `PluginConfig` 配置加载与校验
- i18n 语言包与消息网关基线
- `core.model` / `core.action` / `core.port` 契约
- SQLite 仓储基线与内存仓储回退
- 生命周期、查询、恢复、权限服务骨架
- `/dummy spawn|remove|list|tp|swap|skin|inv|exp|config|action` 可上服联调
- `compat.v120x` / `compat.v121x` 已提供可见实体版测试桥接

## 构建

在工作区根目录执行：

```powershell
mvn -DskipTests package
```

产物位于 `target/` 目录。

## 实际环境测试

推荐测试环境：

- Java 21
- Paper 1.20.6 或 1.21.x

部署步骤：

1. 执行 `mvn -DskipTests package`
2. 将 `target/dummy-0.1.0-SNAPSHOT.jar` 放入服务器 `plugins/` 目录
3. 启动服务器，确认控制台出现 `Dummy 已启用` 或 `Dummy enabled`
4. 使用以下命令进行烟雾测试：
	- `/dummy spawn`
	- `/dummy list`
	- `/dummy tp <dummy> <player>`
	- `/dummy swap <dummy> <player>`
	- `/dummy action <dummy> jump`
	- `/dummy action <dummy> look 180 0`
	- `/dummy action <dummy> run true`
	- `/dummy action <dummy> hold_item diamond_sword 1`
	- `/dummy config <dummy>`
	- `/dummy inv <dummy>`
6. 让 Dummy 攻击并击杀生物后执行 `/dummy exp <dummy> transfer`，确认经验会返还给执行者
7. 直接右键 Dummy，确认会打开可编辑背包，并在关闭后保存内容
8. 潜行右键 Dummy，确认会打开配置界面，并可点击切换 trait
9. 重启服务器后再次执行 `/dummy list`，确认 Dummy 定义、背包数据与已缓存经验已恢复
10. 观察原有 Dummy 是否重新出现在世界中，确认重启恢复链路正常

## 当前可测范围

当前版本已经适合直接上测试服验证以下内容：

- 插件加载与配置读取
- SQLite 文件创建与基本档案读写
- 运行态快照保存与重启后基本恢复
- Dummy 实体生成、移除、传送、交换位置
- 右键可编辑背包、潜行右键 trait 配置 GUI、伤害保护
- 经验积累与 `/dummy exp <dummy> transfer`
- 背包与经验缓存的重启后恢复
- 主人睡觉/起床时，Dummy 可按配置自动同步睡眠状态
- 扩展动作链路：`drop`、`use`、`sleep`、`wake`、`attack`、`dig`、`jump`、`look`、`look_at_entity`、`move`、`run`、`sneak`、`ride`、`swap_hand`、`hold_item`、`execute_command`、`send_chat`

## 已知限制

当前兼容层为了尽快进入真实环境联调，使用的是“可见实体测试桥接”而不是完整 NMS 假玩家实现：

- 现阶段 Dummy 以冻结的 humanoid 测试实体呈现，用于验证指令、持久化和交互主链路
- `skin` 当前通过玩家头部贴图近似表现，不是完整玩家模型换肤
- `run` / `sneak` 等动作目前是可见状态占位，不代表最终 NMS 行为
- `inv` 现已支持编辑并持久化基础物品、护甲与副手；但仍不是与真实玩家实体完全同步的原生背包
- `auto-restock` 当前表现为关闭背包时自动保留原有物品槽位，不会因为误清空而丢失存档物品
- `sleep` / `wake` / `dig` 等动作在测试桥接中提供的是近似行为，主要用于联调命令与持久化链路

如果要进入下一阶段，优先目标应是：

1. 用真实 NMS / Paper 假玩家替换测试桥接
2. 补运行态快照与关服刷新
3. 用真实玩家语义完善动作、经验与背包同步

## 当前状态

当前版本重点是把架构基线和主流程骨架跑通，便于继续迭代以下能力：

1. 真实 NMS / Paper 假玩家实体实现
2. 动作系统内建动作实现
3. 背包 GUI 与右键交互
4. 运行态快照与关闭时刷新
5. 皮肤拉取、权限细化、消息完整覆盖

详细设计见 [dev/architecture.md](dev/architecture.md) 与 [dev/develop.md](dev/develop.md)。
