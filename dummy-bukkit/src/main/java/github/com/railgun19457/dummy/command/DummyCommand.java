package github.com.railgun19457.dummy.command;

import github.com.railgun19457.dummy.DummyPlugin;
import github.com.railgun19457.dummy.common.i18n.I18nService;
import github.com.railgun19457.dummy.core.manager.DummyManager;
import github.com.railgun19457.dummy.api.model.DummySession;
import github.com.railgun19457.dummy.core.scheduler.ActionMode;
import github.com.railgun19457.dummy.core.scheduler.ActionScheduler;
import github.com.railgun19457.dummy.core.scheduler.ActionTask;
import github.com.railgun19457.dummy.gui.DummyGuiService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.bukkit.FluidCollisionMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class DummyCommand implements CommandExecutor, TabCompleter {

    private final DummyPlugin plugin;
    private final DummyManager dummyManager;
    private final ActionScheduler actionScheduler;
    private final DummyGuiService guiService;
    private I18nService i18n;

    public DummyCommand(DummyPlugin plugin, DummyManager dummyManager, ActionScheduler actionScheduler, DummyGuiService guiService, I18nService i18n) {
        this.plugin = plugin;
        this.dummyManager = dummyManager;
        this.actionScheduler = actionScheduler;
        this.guiService = guiService;
        this.i18n = i18n;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(i18n.message("dummy.only_player"));
            return true;
        }

        if (!player.hasPermission("dummy.use")) {
            player.sendMessage(i18n.message("dummy.no_permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "spawn" -> handleSpawn(player, args);
            case "remove" -> handleRemove(player, args);
            case "config" -> handleConfig(player, args);
            case "list" -> handleList(player);
            case "action" -> handleAction(player, args);
            case "tpto" -> handleTpto(player, args);
            case "tps" -> handleTps(player, args);
            case "tphere" -> handleTphere(player, args);
            case "skin" -> handleSkin(player, args);
            case "exp" -> handleExp(player, args);
            case "inv" -> handleInv(player, args);
            case "reload" -> handleReload(player);
            default -> sendHelp(player);
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                return Collections.emptyList();
            }
            return List.of("spawn", "remove", "config", "list", "action", "tpto", "tps", "tphere", "skin", "exp", "inv", "reload").stream()
                    .filter(value -> hasSubPermission(player, value))
                    .filter(value -> value.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && ("remove".equalsIgnoreCase(args[0])
            || "action".equalsIgnoreCase(args[0])
            || "config".equalsIgnoreCase(args[0])
            || "tpto".equalsIgnoreCase(args[0])
            || "tps".equalsIgnoreCase(args[0])
            || "tphere".equalsIgnoreCase(args[0])
            || "skin".equalsIgnoreCase(args[0])
            || "exp".equalsIgnoreCase(args[0])
            || "inv".equalsIgnoreCase(args[0])) && sender instanceof Player player) {
            return dummyManager.listFor(player).stream()
                    .map(DummySession::name)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        if (args.length == 3 && "skin".equalsIgnoreCase(args[0])) {
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT)))
                .collect(Collectors.toCollection(ArrayList::new));
        }

        if (args.length == 3 && "action".equalsIgnoreCase(args[0])) {
            return List.of("stop", "drop", "use", "sleep", "wakeup", "attack", "mine", "jump", "look", "move", "sprint", "sneak", "mount", "swap", "hold", "chat", "command").stream()
                    .filter(value -> value.startsWith(args[2].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        if (args.length == 4 && "action".equalsIgnoreCase(args[0]) && !"stop".equalsIgnoreCase(args[2])) {
            return List.of("once", "continuous", "interval:20").stream()
                    .filter(value -> value.startsWith(args[3].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    private void handleSpawn(Player player, String[] args) {
        if (!checkPermission(player, "dummy.command.spawn")) {
            return;
        }
        String requestedName = args.length >= 2 ? args[1] : null;
        DummyManager.OperationResult result = dummyManager.spawn(player, requestedName);
        if (!result.success()) {
            player.sendMessage("§c[Dummy] " + result.message());
            return;
        }

        player.sendMessage(i18n.message("dummy.spawn.success", Map.of("name", result.session().name())));
        if (result.warning() != null && !result.warning().isEmpty()) {
            player.sendMessage("§e[Dummy] " + result.warning());
        }
    }

    private void handleRemove(Player player, String[] args) {
        if (!checkPermission(player, "dummy.command.remove")) {
            return;
        }
        if (args.length < 2) {
            player.sendMessage("§c[Dummy] 用法: /dummy remove <name>");
            return;
        }

        DummyManager.OperationResult result = dummyManager.remove(player, args[1]);
        if (!result.success()) {
            player.sendMessage("§c[Dummy] " + result.message());
            return;
        }

        player.sendMessage(i18n.message("dummy.remove.success", Map.of("name", result.session().name())));
    }

    private void handleConfig(Player player, String[] args) {
        if (!checkPermission(player, "dummy.command.config")) {
            return;
        }
        if (args.length < 2) {
            player.sendMessage("§c[Dummy] 用法: /dummy config <name>");
            return;
        }

        DummySession session = dummyManager.findByName(args[1]).orElse(null);
        if (session == null) {
            player.sendMessage("§c[Dummy] 未找到假人: " + args[1]);
            return;
        }
        if (!dummyManager.canManage(player, session)) {
            player.sendMessage(i18n.message("dummy.action.not_owner"));
            return;
        }

        guiService.openConfigGui(player, session);
    }

    private void handleList(Player player) {
        if (!checkPermission(player, "dummy.command.list")) {
            return;
        }
        List<DummySession> sessions = dummyManager.listFor(player);
        if (sessions.isEmpty()) {
            player.sendMessage(i18n.message("dummy.list.empty"));
            return;
        }

        player.sendMessage(i18n.message("dummy.list.title", Map.of("size", String.valueOf(sessions.size()))));
        for (DummySession session : sessions) {
            player.sendMessage(i18n.message("dummy.list.item", Map.of(
                    "name", session.name(),
                    "owner", session.ownerName()
            )));
        }
    }

    private void handleAction(Player sender, String[] args) {
        if (!checkPermission(sender, "dummy.command.action")) {
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("§c[Dummy] 用法: /dummy action <name> <action|stop> [mode] [action args...]");
            return;
        }

        String dummyName = args[1];
        DummySession session = dummyManager.findByName(dummyName).orElse(null);
        if (session == null) {
            sender.sendMessage(i18n.message("dummy.action.not_found", Map.of("name", dummyName)));
            return;
        }

        if (!dummyManager.canManage(sender, session)) {
            sender.sendMessage(i18n.message("dummy.action.not_owner"));
            return;
        }

        Player target = Bukkit.getPlayerExact(session.name());
        if (target == null || !target.isOnline()) {
            sender.sendMessage(i18n.message("dummy.action.target_offline"));
            return;
        }

        String actionName = args[2].toLowerCase(Locale.ROOT);
        if ("stop".equals(actionName)) {
            boolean stopped = actionScheduler.stop(target.getUniqueId());
            sender.sendMessage(stopped ? i18n.message("dummy.action.stop.success") : i18n.message("dummy.action.stop.none"));
            return;
        }

        if (args.length < 4) {
            sender.sendMessage("§c[Dummy] 用法: /dummy action <name> <action> <once|continuous|interval:ticks> [args...]");
            return;
        }

        ActionMode mode;
        try {
            mode = ActionMode.parse(args[3]);
        } catch (Exception exception) {
            sender.sendMessage("§c[Dummy] 无效执行模式: " + args[3]);
            return;
        }

        ActionTask task = buildActionTask(sender, actionName, args, 4);
        if (task == null) {
            return;
        }

        actionScheduler.schedule(target, task, mode);
        sender.sendMessage(i18n.message("dummy.action.set.success", Map.of(
            "action", actionName,
            "mode", mode.type().name().toLowerCase(Locale.ROOT)
        )));
    }

    private void handleTpto(Player sender, String[] args) {
        if (!checkPermission(sender, "dummy.command.tpto")) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§c[Dummy] 用法: /dummy tpto <name>");
            return;
        }
        Player target = resolveOnlineDummy(sender, args[1]);
        if (target == null) {
            return;
        }
        sender.teleport(target.getLocation());
        dummyManager.syncSessionPosition(target.getName(), target.getLocation());
        sender.sendMessage("§a[Dummy] 已传送到假人 §f" + target.getName());
    }

    private void handleTps(Player sender, String[] args) {
        if (!checkPermission(sender, "dummy.command.tps")) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§c[Dummy] 用法: /dummy tps <name>");
            return;
        }
        Player target = resolveOnlineDummy(sender, args[1]);
        if (target == null) {
            return;
        }
        var senderLoc = sender.getLocation().clone();
        var targetLoc = target.getLocation().clone();
        sender.teleport(targetLoc);
        target.teleport(senderLoc);
        dummyManager.syncSessionPosition(target.getName(), senderLoc);
        sender.sendMessage("§a[Dummy] 已与假人交换位置: §f" + target.getName());
    }

    private void handleTphere(Player sender, String[] args) {
        if (!checkPermission(sender, "dummy.command.tphere")) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§c[Dummy] 用法: /dummy tphere <name>");
            return;
        }
        Player target = resolveOnlineDummy(sender, args[1]);
        if (target == null) {
            return;
        }
        target.teleport(sender.getLocation());
        dummyManager.syncSessionPosition(target.getName(), sender.getLocation());
        sender.sendMessage("§a[Dummy] 已将假人传送到你身边: §f" + target.getName());
    }

    private void handleSkin(Player sender, String[] args) {
        if (!checkPermission(sender, "dummy.command.skin")) {
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("§c[Dummy] 用法: /dummy skin <name> <playername>");
            return;
        }

        DummyManager.OperationResult result = dummyManager.updateSkin(sender, args[1], args[2]);
        if (!result.success()) {
            sender.sendMessage("§c[Dummy] " + result.message());
            return;
        }
        sender.sendMessage("§a[Dummy] 皮肤来源已更新为 §f" + args[2] + "§a（下次生成时生效）");
    }

    private void handleExp(Player sender, String[] args) {
        if (!checkPermission(sender, "dummy.command.exp")) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§c[Dummy] 用法: /dummy exp <name>");
            return;
        }
        Player target = resolveOnlineDummy(sender, args[1]);
        if (target == null) {
            return;
        }

        int levels = target.getLevel();
        int progressExp = Math.round(target.getExpToLevel() * target.getExp());
        if (levels <= 0 && progressExp <= 0) {
            sender.sendMessage("§e[Dummy] 该假人没有可转移经验。");
            return;
        }

        target.setLevel(0);
        target.setExp(0f);
        target.setTotalExperience(0);
        sender.giveExpLevels(levels);
        sender.giveExp(progressExp);
        sender.sendMessage("§a[Dummy] 已转移经验: §f" + levels + " 级 + " + progressExp + " 点经验");
    }

    private void handleInv(Player sender, String[] args) {
        if (!checkPermission(sender, "dummy.command.inv")) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§c[Dummy] 用法: /dummy inv <name>");
            return;
        }
        Player target = resolveOnlineDummy(sender, args[1]);
        if (target == null) {
            return;
        }
        sender.openInventory(target.getInventory());
        sender.sendMessage("§a[Dummy] 已打开假人背包: §f" + target.getName());
    }

    private void handleReload(Player sender) {
        if (!checkPermission(sender, "dummy.command.reload")) {
            return;
        }
        plugin.reloadRuntime();
        this.i18n = plugin.i18nService();
        sender.sendMessage("§a[Dummy] 配置与语言已重载。");
    }

    private Player resolveOnlineDummy(Player sender, String dummyName) {
        DummySession session = dummyManager.findByName(dummyName).orElse(null);
        if (session == null) {
            sender.sendMessage(i18n.message("dummy.action.not_found", Map.of("name", dummyName)));
            return null;
        }
        if (!dummyManager.canManage(sender, session)) {
            sender.sendMessage(i18n.message("dummy.action.not_owner"));
            return null;
        }
        Player target = Bukkit.getPlayerExact(session.name());
        if (target == null || !target.isOnline()) {
            sender.sendMessage(i18n.message("dummy.action.target_offline"));
            return null;
        }
        return target;
    }

    private ActionTask buildActionTask(Player sender, String actionName, String[] args, int actionArgStart) {
        return switch (actionName) {
            case "drop" -> player -> player.dropItem(false);
            case "use" -> Player::swingMainHand;
            case "sleep" -> player -> {
                try {
                    player.sleep(player.getLocation(), true);
                } catch (Throwable ignored) {
                }
            };
            case "wakeup" -> player -> player.wakeup(true);
            case "attack" -> player -> {
                Entity target = player.getNearbyEntities(4, 3, 4).stream()
                        .filter(entity -> entity.getUniqueId() != player.getUniqueId())
                        .findFirst()
                        .orElse(null);
                if (target != null) {
                    player.attack(target);
                }
            };
            case "mine" -> player -> {
                Block block = player.rayTraceBlocks(4, FluidCollisionMode.NEVER)
                        .getHitBlock();
                if (block != null && block.getType() != Material.AIR) {
                    block.breakNaturally(player.getInventory().getItemInMainHand());
                }
            };
            case "jump" -> player -> {
                Vector velocity = player.getVelocity();
                velocity.setY(Math.max(0.42D, velocity.getY()));
                player.setVelocity(velocity);
            };
            case "look" -> {
                if (args.length < actionArgStart + 2) {
                    sender.sendMessage("§c[Dummy] look 参数: <yaw> <pitch>");
                    yield null;
                }
                float yaw;
                float pitch;
                try {
                    yaw = Float.parseFloat(args[actionArgStart]);
                    pitch = Float.parseFloat(args[actionArgStart + 1]);
                } catch (NumberFormatException exception) {
                    sender.sendMessage("§c[Dummy] yaw/pitch 必须是数字。");
                    yield null;
                }
                yield player -> {
                    var loc = player.getLocation();
                    loc.setYaw(yaw);
                    loc.setPitch(pitch);
                    player.teleport(loc);
                };
            }
            case "move" -> {
                double speed = 0.25D;
                if (args.length > actionArgStart) {
                    try {
                        speed = Double.parseDouble(args[actionArgStart]);
                    } catch (NumberFormatException exception) {
                        sender.sendMessage("§e[Dummy] move 速度参数非法，使用默认 0.25。");
                    }
                }
                final double finalSpeed = speed;
                yield player -> {
                    Vector direction = player.getLocation().getDirection().setY(0).normalize().multiply(finalSpeed);
                    player.setVelocity(direction);
                };
            }
            case "sneak" -> {
                boolean value = true;
                if (args.length > actionArgStart) {
                    String raw = args[actionArgStart].toLowerCase(Locale.ROOT);
                    if (!"true".equals(raw) && !"false".equals(raw)) {
                        sender.sendMessage("§c[Dummy] sneak 参数必须为 true 或 false。");
                        yield null;
                    }
                    value = Boolean.parseBoolean(raw);
                }
                final boolean finalValue = value;
                yield player -> player.setSneaking(finalValue);
            }
            case "sprint" -> {
                boolean value = true;
                if (args.length > actionArgStart) {
                    String raw = args[actionArgStart].toLowerCase(Locale.ROOT);
                    if (!"true".equals(raw) && !"false".equals(raw)) {
                        sender.sendMessage("§c[Dummy] sprint 参数必须为 true 或 false。");
                        yield null;
                    }
                    value = Boolean.parseBoolean(raw);
                }
                final boolean finalValue = value;
                yield player -> player.setSprinting(finalValue);
            }
            case "swap" -> player -> {
                var inv = player.getInventory();
                ItemStack main = inv.getItemInMainHand();
                ItemStack off = inv.getItemInOffHand();
                inv.setItemInMainHand(off);
                inv.setItemInOffHand(main);
            };
            case "hold" -> {
                if (args.length <= actionArgStart) {
                    sender.sendMessage("§c[Dummy] hold 参数: <material>");
                    yield null;
                }
                Material material = Material.matchMaterial(args[actionArgStart]);
                if (material == null) {
                    sender.sendMessage("§c[Dummy] 未知物品类型: " + args[actionArgStart]);
                    yield null;
                }
                yield player -> player.getInventory().setItemInMainHand(new ItemStack(material));
            }
            case "chat" -> {
                if (args.length <= actionArgStart) {
                    sender.sendMessage("§c[Dummy] chat 参数: <message...>");
                    yield null;
                }
                String message = String.join(" ", Arrays.copyOfRange(args, actionArgStart, args.length));
                yield player -> player.chat(message);
            }
            case "command" -> {
                if (args.length <= actionArgStart) {
                    sender.sendMessage("§c[Dummy] command 参数: <cmd...>");
                    yield null;
                }
                String commandLine = String.join(" ", Arrays.copyOfRange(args, actionArgStart, args.length));
                yield player -> Bukkit.dispatchCommand(player, commandLine);
            }
            case "mount" -> player -> {
                Entity target = player.getNearbyEntities(4, 2, 4).stream().findFirst().orElse(null);
                if (target != null) {
                    target.addPassenger(player);
                }
            };
            default -> {
                sender.sendMessage("§c[Dummy] 未实现动作: " + actionName);
                yield null;
            }
        };
    }

    private void sendHelp(Player player) {
        player.sendMessage(i18n.message("dummy.help.title"));
        if (hasSubPermission(player, "spawn")) player.sendMessage(i18n.message("dummy.help.spawn"));
        if (hasSubPermission(player, "remove")) player.sendMessage(i18n.message("dummy.help.remove"));
        if (hasSubPermission(player, "config")) player.sendMessage("§7- /dummy config <name>");
        if (hasSubPermission(player, "list")) player.sendMessage(i18n.message("dummy.help.list"));
        if (hasSubPermission(player, "action")) player.sendMessage(i18n.message("dummy.help.action"));
        if (hasSubPermission(player, "tpto")) player.sendMessage("§7- /dummy tpto <name>");
        if (hasSubPermission(player, "tps")) player.sendMessage("§7- /dummy tps <name>");
        if (hasSubPermission(player, "tphere")) player.sendMessage("§7- /dummy tphere <name>");
        if (hasSubPermission(player, "skin")) player.sendMessage("§7- /dummy skin <name> <playername>");
        if (hasSubPermission(player, "exp")) player.sendMessage("§7- /dummy exp <name>");
        if (hasSubPermission(player, "inv")) player.sendMessage("§7- /dummy inv <name>");
        if (hasSubPermission(player, "reload")) player.sendMessage("§7- /dummy reload");
    }

    private boolean checkPermission(Player player, String permission) {
        if (player.hasPermission("dummy.admin") || player.hasPermission(permission)) {
            return true;
        }
        player.sendMessage(i18n.message("dummy.no_permission"));
        return false;
    }

    private boolean hasSubPermission(Player player, String subCommand) {
        if (player.hasPermission("dummy.admin")) {
            return true;
        }
        return player.hasPermission("dummy.command." + subCommand);
    }
}
