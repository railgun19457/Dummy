package dev.dummy.command;

import dev.dummy.DummyPlugin;
import dev.dummy.action.DummyActionService;
import dev.dummy.dummy.DummyInstance;
import dev.dummy.dummy.DummyManager;
import dev.dummy.dummy.DummySkin;
import dev.dummy.gui.DummyGuiListener;
import dev.dummy.i18n.I18n;
import dev.dummy.i18n.LocalizedException;
import dev.dummy.skin.SkinService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class DummyCommand implements BasicCommand {
    private static final List<String> SUBCOMMANDS = List.of(
            "spawn", "remove", "list", "reload", "config", "skin", "exp", "inv", "tpto", "tphere", "tps", "actions"
    );
    private static final List<String> CONFIG_KEYS = List.of("invulnerable", "collision", "ghost", "chunk-loader", "show-in-tab", "name-format");
    private static final List<String> ACTIONS = List.of(
            "attack", "chat", "command", "drop", "hold", "jump", "look", "lookat", "mine", "mount", "move", "place", "sneak", "swap", "use", "stop"
    );
    private static final List<String> MODE_ACTIONS = List.of("attack", "drop", "jump", "look", "mine", "move", "place", "use");
    private static final List<String> TOGGLE_ACTIONS = List.of("sneak");
    private static final List<String> LOOK_ARGS = List.of("direction", "entity", "angle");
    private static final List<String> LOOK_DIRECTIONS = List.of("east", "west", "north", "south");
    private static final List<String> LOOK_ENTITY_TYPES = List.of("player", "monster");
    private static final List<String> LOOK_ANGLE_ARGS = List.of("~", "0");
    private static final List<String> TOGGLE_ARGS = List.of("toggle", "on", "off");
    private static final List<String> REPEAT_OPTIONS = List.of("interval:", "duration:");

    private final DummyPlugin plugin;
    private final DummyManager dummyManager;
    private final SkinService skinService;
    private final DummyActionService actionService;
    private final I18n i18n;
    private final DummyGuiListener guiListener;

    public DummyCommand(DummyPlugin plugin, DummyManager dummyManager, SkinService skinService, DummyActionService actionService, I18n i18n, DummyGuiListener guiListener) {
        this.plugin = plugin;
        this.dummyManager = dummyManager;
        this.skinService = skinService;
        this.actionService = actionService;
        this.i18n = i18n;
        this.guiListener = guiListener;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (args.length == 0) {
            sendUsage(sender);
            return;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        try {
            switch (subcommand) {
                case "spawn" -> spawn(sender, args);
                case "remove" -> remove(sender, args);
                case "list" -> list(sender);
                case "reload" -> reload(sender);
                case "config" -> config(sender, args);
                case "skin" -> skin(sender, args);
                case "exp" -> exp(sender, args);
                case "inv" -> inv(sender, args);
                case "tpto" -> tpto(sender, args);
                case "tphere" -> tphere(sender, args);
                case "tps" -> tps(sender, args);
                case "actions" -> actions(sender, args);
                default -> sendUsage(sender);
            }
        } catch (LocalizedException ex) {
            sender.sendMessage(i18n.component(ex.key(), NamedTextColor.RED, ex.args()));
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(Component.text(ex.getMessage(), NamedTextColor.RED));
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Command failed: " + ex.getMessage());
            sender.sendMessage(i18n.component("error.command-failed", NamedTextColor.RED, ex.getMessage()));
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length == 0) {
            return SUBCOMMANDS;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("remove")) {
            return removeSuggestions(source.getSender(), "");
        }
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            return removeSuggestions(source.getSender(), args[1]);
        }
        if (args.length == 2 && needsActiveDummyName(args[0])) {
            return filter(dummyManager.activeNames(source.getSender()), args[1]);
        }
        if (args.length == 2 && needsDummyName(args[0])) {
            return filter(dummyManager.names(source.getSender()), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("config")) {
            return filter(CONFIG_KEYS, args[2]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("config") && !args[2].equalsIgnoreCase("name-format")) {
            return filter(List.of("true", "false"), args[3]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("skin")) {
            return filter(List.of("set", "clear"), args[2]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("skin") && args[2].equalsIgnoreCase("set")) {
            return filter(plugin.getServer().getOnlinePlayers().stream().map(Player::getName).toList(), args[3]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("exp")) {
            return filter(List.of("all"), args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("actions")) {
            return filter(ACTIONS, args[2]);
        }
        if (args.length >= 4 && args[0].equalsIgnoreCase("actions")) {
            return actionSuggestions(args);
        }
        return List.of();
    }

    @Override
    public String permission() {
        return "dummy.command";
    }

    private void spawn(CommandSender sender, String[] args) {
        requirePermission(sender, "dummy.command.spawn");
        if (!(sender instanceof Player player)) {
            throw new LocalizedException("error.player-only-current-location");
        }
        if (args.length != 2) {
            message(sender, "usage.spawn", NamedTextColor.YELLOW);
            return;
        }
        DummyInstance dummy = dummyManager.spawn(sender, args[1], player.getLocation(), skinService.skinFromPlayer(player));
        message(sender, "command.spawn-success", NamedTextColor.GREEN, dummy.name());
    }

    private void remove(CommandSender sender, String[] args) {
        requirePermission(sender, "dummy.command.remove");
        if (args.length != 2) {
            message(sender, "usage.remove", NamedTextColor.YELLOW);
            return;
        }
        String reason = i18n.tr("command.remove-reason", sender.getName());
        if (args[1].equalsIgnoreCase("all")) {
            int removed = dummyManager.removeAll(sender, reason);
            message(sender, "command.remove-all-success", NamedTextColor.GREEN, removed);
            return;
        }
        requireManaged(sender, args[1]);
        boolean removed = dummyManager.remove(args[1], reason);
        if (removed) {
            message(sender, "command.remove-success", NamedTextColor.GREEN, args[1]);
        } else {
            message(sender, "error.dummy-not-found", NamedTextColor.RED, args[1]);
        }
    }

    private void list(CommandSender sender) {
        requirePermission(sender, "dummy.command.list");
        List<String> names = dummyManager.names(sender);
        if (names.isEmpty()) {
            message(sender, "command.list-empty", NamedTextColor.GRAY);
            return;
        }
        message(sender, "command.list", NamedTextColor.GREEN, String.join(", ", names));
    }

    private void reload(CommandSender sender) {
        requirePermission(sender, "dummy.command.reload");
        plugin.reloadDummyConfig();
        dummyManager.save();
        message(sender, "command.reload-success", NamedTextColor.GREEN, i18n.language());
    }

    private void config(CommandSender sender, String[] args) {
        requirePermission(sender, "dummy.command.config");
        if (args.length == 2) {
            Player player = requirePlayer(sender);
            guiListener.openConfigMenu(player, requireManaged(sender, args[1]));
            return;
        }
        if (args.length < 4) {
            message(sender, "usage.config", NamedTextColor.YELLOW);
            return;
        }
        requireManaged(sender, args[1]);
        String value = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        dummyManager.updateSettings(args[1], args[2].toLowerCase(Locale.ROOT), value);
        message(sender, "command.config-updated", NamedTextColor.GREEN, args[1], args[2], value);
    }

    private void skin(CommandSender sender, String[] args) {
        requirePermission(sender, "dummy.command.skin");
        if (args.length < 3) {
            message(sender, "usage.skin", NamedTextColor.YELLOW);
            return;
        }
        requireManaged(sender, args[1]);
        switch (args[2].toLowerCase(Locale.ROOT)) {
            case "clear" -> {
                if (args.length != 3) {
                    throw new LocalizedException("usage.skin");
                }
                dummyManager.setSkin(args[1], DummySkin.NONE);
                message(sender, "command.skin-cleared", NamedTextColor.GREEN, args[1]);
            }
            case "set" -> {
                if (args.length != 4) {
                    throw new LocalizedException("usage.skin-set");
                }
                message(sender, "command.skin-fetching", NamedTextColor.GRAY, args[3]);
                skinService.fetchPlayerSkin(args[3]).whenComplete((skin, throwable) -> plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (throwable != null) {
                        message(sender, "command.skin-fetch-failed", NamedTextColor.RED, failureMessage(throwable));
                        return;
                    }
                    try {
                        requireManaged(sender, args[1]);
                    } catch (LocalizedException ex) {
                        message(sender, ex.key(), NamedTextColor.RED, ex.args());
                        return;
                    }
                    dummyManager.setSkin(args[1], skin);
                    message(sender, "command.skin-set-updated", NamedTextColor.GREEN, args[1], args[3]);
                }));
            }
            default -> throw new LocalizedException("error.unknown-skin-mode", args[2]);
        }
    }

    private void exp(CommandSender sender, String[] args) {
        requirePermission(sender, "dummy.command.exp");
        Player player = requirePlayer(sender);
        if (args.length != 3) {
            message(sender, "usage.exp", NamedTextColor.YELLOW);
            return;
        }
        requireManaged(sender, args[1]);
        boolean all = args[2].equalsIgnoreCase("all");
        int amount = all ? 0 : Integer.parseInt(args[2]);
        int transferred = dummyManager.transferExperience(args[1], player, all, amount);
        message(sender, "command.exp-transferred", NamedTextColor.GREEN, transferred);
    }

    private void inv(CommandSender sender, String[] args) {
        requirePermission(sender, "dummy.command.inv");
        Player player = requirePlayer(sender);
        if (args.length != 2) {
            message(sender, "usage.inv", NamedTextColor.YELLOW);
            return;
        }
        guiListener.openDummyInventory(player, requireManaged(sender, args[1]));
    }

    private void tpto(CommandSender sender, String[] args) {
        requirePermission(sender, "dummy.command.tpto");
        Player player = requirePlayer(sender);
        if (args.length != 2) {
            message(sender, "usage.tpto", NamedTextColor.YELLOW);
            return;
        }
        requireManaged(sender, args[1]);
        dummyManager.teleportPlayerToDummy(player, args[1]);
        message(sender, "command.tpto-success", NamedTextColor.GREEN, args[1]);
    }

    private void tphere(CommandSender sender, String[] args) {
        requirePermission(sender, "dummy.command.tphere");
        Player player = requirePlayer(sender);
        if (args.length != 2) {
            message(sender, "usage.tphere", NamedTextColor.YELLOW);
            return;
        }
        requireManaged(sender, args[1]);
        dummyManager.teleportDummy(args[1], player.getLocation());
        message(sender, "command.tphere-success", NamedTextColor.GREEN, args[1]);
    }

    private void tps(CommandSender sender, String[] args) {
        requirePermission(sender, "dummy.command.tps");
        Player player = requirePlayer(sender);
        if (args.length != 2) {
            message(sender, "usage.tps", NamedTextColor.YELLOW);
            return;
        }
        requireManaged(sender, args[1]);
        dummyManager.swap(player, args[1]);
        message(sender, "command.tps-success", NamedTextColor.GREEN, args[1]);
    }

    private void actions(CommandSender sender, String[] args) {
        requirePermission(sender, "dummy.command.actions");
        if (args.length < 3) {
            message(sender, "usage.actions", NamedTextColor.YELLOW);
            return;
        }
        DummyInstance dummy = requireManaged(sender, args[1]);
        String action = args[2].toLowerCase(Locale.ROOT);
        if (action.equals("stop")) {
            int stopped = actionService.stop(dummy, args.length >= 4 ? args[3] : null);
            message(sender, "command.actions-stopped", NamedTextColor.GREEN, stopped);
            return;
        }

        ActionRequest request = parseActionRequest(action, DummyActionService.tail(args, 3));
        actionService.run(dummy, action, request.args(), request.repeat(), request.interval(), request.duration());
        message(sender, "command.actions-started", NamedTextColor.GREEN, action, dummy.name());
    }

    private void sendUsage(CommandSender sender) {
        message(sender, "usage.root", NamedTextColor.YELLOW);
    }

    private void requirePermission(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) {
            throw new LocalizedException("error.no-permission", permission);
        }
    }

    private DummyInstance requireManaged(CommandSender sender, String name) {
        DummyInstance dummy = dummyManager.require(name);
        if (!dummyManager.canManage(sender, dummy)) {
            throw new LocalizedException("error.not-dummy-owner", name);
        }
        return dummy;
    }

    private List<String> filter(List<String> values, String prefix) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(normalized))
                .toList();
    }

    private List<String> removeSuggestions(CommandSender sender, String prefix) {
        List<String> names = new ArrayList<>(dummyManager.names(sender));
        if (sender.hasPermission("dummy.command.remove")) {
            names.add("all");
        }
        return filter(names, prefix);
    }

    private boolean needsDummyName(String subcommand) {
        return List.of("remove", "config", "skin", "exp", "inv", "tpto", "tphere", "tps", "actions").contains(subcommand.toLowerCase(Locale.ROOT));
    }

    private boolean needsActiveDummyName(String subcommand) {
        return List.of("config", "skin", "exp", "inv", "tpto", "tphere", "tps", "actions").contains(subcommand.toLowerCase(Locale.ROOT));
    }

    private List<String> actionSuggestions(String[] args) {
        String action = args[2].toLowerCase(Locale.ROOT);
        if (action.equals("stop")) {
            return args.length == 4 ? filter(ACTIONS, args[3]) : List.of();
        }
        if (action.equals("look")) {
            return lookActionSuggestions(args);
        }
        if (args.length == 4) {
            List<String> values = new ArrayList<>();
            if (supportsMode(action)) {
                values.add("once");
                values.add("repeat");
            }
            values.addAll(actionArgumentSuggestions(action));
            return filter(values, args[3]);
        }
        int repeatIndex = modeIndex(args, 3, "repeat");
        if (repeatIndex >= 3) {
            return repeatOptionSuggestions(args, repeatIndex);
        }
        if (args[3].equalsIgnoreCase("once")) {
            return args.length == 5 ? filter(actionArgumentSuggestions(action), args[4]) : List.of();
        }
        if (supportsMode(action)) {
            return filter(List.of("repeat"), args[args.length - 1]);
        }
        return List.of();
    }

    private List<String> lookActionSuggestions(String[] args) {
        int repeatIndex = modeIndex(args, 3, "repeat");
        if (repeatIndex >= 3) {
            return repeatOptionSuggestions(args, repeatIndex);
        }
        int start = args[3].equalsIgnoreCase("once") ? 4 : 3;
        boolean once = start == 4;
        if (args.length == start) {
            return LOOK_ARGS;
        }
        return lookArgumentSuggestions(args, start, once);
    }

    private List<String> lookArgumentSuggestions(String[] args, int start, boolean once) {
        if (args.length == start + 1) {
            return filter(LOOK_ARGS, args[start]);
        }

        String branch = args[start].toLowerCase(Locale.ROOT);
        return switch (branch) {
            case "direction" -> lookDirectionSuggestions(args, start, once);
            case "entity" -> lookEntitySuggestions(args, start, once);
            case "angle" -> lookAngleSuggestions(args, start, once);
            default -> List.of();
        };
    }

    private List<String> lookDirectionSuggestions(String[] args, int start, boolean once) {
        if (args.length == start + 2) {
            return filter(LOOK_DIRECTIONS, args[start + 1]);
        }
        if (!once && args.length == start + 3) {
            return filter(List.of("repeat"), args[start + 2]);
        }
        return List.of();
    }

    private List<String> lookEntitySuggestions(String[] args, int start, boolean once) {
        if (args.length == start + 2) {
            List<String> values = new ArrayList<>(LOOK_ENTITY_TYPES);
            if (!once) {
                values.add("repeat");
            }
            return filter(values, args[start + 1]);
        }

        String entityType = args[start + 1].toLowerCase(Locale.ROOT);
        if (entityType.equals("player")) {
            if (args.length == start + 3) {
                List<String> values = new ArrayList<>(plugin.getServer().getOnlinePlayers().stream().map(Player::getName).toList());
                if (!once) {
                    values.add("repeat");
                }
                return filter(values, args[start + 2]);
            }
            if (!once && args.length == start + 4) {
                return filter(List.of("repeat"), args[start + 3]);
            }
        }
        if (!once && entityType.equals("monster") && args.length == start + 3) {
            return filter(List.of("repeat"), args[start + 2]);
        }
        return List.of();
    }

    private List<String> lookAngleSuggestions(String[] args, int start, boolean once) {
        if (args.length == start + 2) {
            return filter(LOOK_ANGLE_ARGS, args[start + 1]);
        }
        if (args.length == start + 3) {
            return filter(LOOK_ANGLE_ARGS, args[start + 2]);
        }
        if (!once && args.length == start + 4) {
            return filter(List.of("repeat"), args[start + 3]);
        }
        return List.of();
    }

    private List<String> repeatOptionSuggestions(String[] args, int repeatIndex) {
        List<String> values = new ArrayList<>(REPEAT_OPTIONS);
        for (int i = repeatIndex + 1; i < args.length - 1; i++) {
            String option = args[i].toLowerCase(Locale.ROOT);
            if (option.startsWith("interval:")) {
                values.remove("interval:");
            } else if (option.startsWith("duration:")) {
                values.remove("duration:");
            }
        }
        return filter(values, args[args.length - 1]);
    }

    private boolean supportsMode(String action) {
        return MODE_ACTIONS.contains(action);
    }

    private ActionRequest parseActionRequest(String action, String[] rawArgs) {
        if (!supportsMode(action)) {
            return new ActionRequest(false, DummyActionService.defaultRepeatInterval(action), -1, rawArgs);
        }
        int onceIndex = modeIndex(rawArgs, 0, "once");
        if (onceIndex >= 0) {
            String[] actionArgs = onceIndex == 0 ? DummyActionService.tail(rawArgs, 1) : Arrays.copyOfRange(rawArgs, 0, onceIndex);
            return new ActionRequest(false, DummyActionService.defaultRepeatInterval(action), -1, actionArgs);
        }

        int repeatIndex = modeIndex(rawArgs, 0, "repeat");
        if (repeatIndex >= 0) {
            return parseRepeat(action, rawArgs, repeatIndex);
        }
        return new ActionRequest(false, DummyActionService.defaultRepeatInterval(action), -1, rawArgs);
    }

    private ActionRequest parseRepeat(String action, String[] rawArgs, int repeatIndex) {
        int interval = DummyActionService.defaultRepeatInterval(action);
        int duration = -1;
        for (int i = repeatIndex + 1; i < rawArgs.length; i++) {
            String option = rawArgs[i].toLowerCase(Locale.ROOT);
            if (option.startsWith("interval:")) {
                interval = parseRepeatOption(rawArgs[i], "interval:");
            } else if (option.startsWith("duration:")) {
                duration = parseRepeatOption(rawArgs[i], "duration:");
            } else {
                throw new LocalizedException("error.unknown-repeat-option", rawArgs[i]);
            }
        }
        return new ActionRequest(true, interval, duration, Arrays.copyOfRange(rawArgs, 0, repeatIndex));
    }

    private int modeIndex(String[] args, int from, String mode) {
        for (int i = from; i < args.length; i++) {
            if (args[i].equalsIgnoreCase(mode)) {
                return i;
            }
        }
        return -1;
    }

    private List<String> actionArgumentSuggestions(String action) {
        if (action.equals("look")) {
            return LOOK_ARGS;
        }
        if (action.equals("lookat")) {
            return List.of("~", "<x>");
        }
        if (TOGGLE_ACTIONS.contains(action)) {
            return TOGGLE_ARGS;
        }
        if (action.equals("hold")) {
            return List.of("0", "1", "2", "3", "4", "5", "6", "7", "8");
        }
        if (action.equals("move")) {
            return List.of("slow", "walk", "sprint");
        }
        return List.of();
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        throw new LocalizedException("error.player-only");
    }

    private int parseInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            throw new LocalizedException("error.invalid-number", value);
        }
    }

    private int parseRepeatOption(String option, String prefix) {
        String value = option.substring(prefix.length());
        if (value.isBlank()) {
            throw new LocalizedException("error.repeat-option-value", prefix);
        }
        int ticks = parseInteger(value);
        if (ticks <= 0) {
            throw new LocalizedException("error.invalid-number", value);
        }
        return ticks;
    }

    private void message(CommandSender sender, String key, NamedTextColor color, Object... args) {
        sender.sendMessage(i18n.component(key, color, args));
    }

    private String failureMessage(Throwable throwable) {
        Throwable cause = throwable.getCause();
        return cause == null ? throwable.getMessage() : cause.getMessage();
    }

    private record ActionRequest(boolean repeat, int interval, int duration, String[] args) {
    }
}
