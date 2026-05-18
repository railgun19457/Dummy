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
            "attack", "chat", "command", "drop", "hold", "jump", "look", "lookat", "mine", "mount", "move", "place", "sneak", "sprint", "swap", "use", "stop"
    );
    private static final List<String> MODE_ACTIONS = List.of("attack", "drop", "jump", "look", "mine", "move", "place", "use");
    private static final List<String> TOGGLE_ACTIONS = List.of("sneak", "sprint");
    private static final List<String> LOOK_ARGS = List.of("north", "east", "south", "west", "entity");
    private static final List<String> TOGGLE_ARGS = List.of("toggle", "on", "off");

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
            return filter(dummyManager.activeNames(), args[1]);
        }
        if (args.length == 2 && needsDummyName(args[0])) {
            return filter(dummyManager.names(), args[1]);
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
            requirePermission(sender, "dummy.command.remove-all");
            int removed = dummyManager.removeAll(reason);
            message(sender, "command.remove-all-success", NamedTextColor.GREEN, removed);
            return;
        }
        boolean removed = dummyManager.remove(args[1], reason);
        if (removed) {
            message(sender, "command.remove-success", NamedTextColor.GREEN, args[1]);
        } else {
            message(sender, "error.dummy-not-found", NamedTextColor.RED, args[1]);
        }
    }

    private void list(CommandSender sender) {
        requirePermission(sender, "dummy.command.list");
        List<String> names = dummyManager.names();
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
            guiListener.openConfigMenu(player, dummyManager.require(args[1]));
            return;
        }
        if (args.length < 4) {
            message(sender, "usage.config", NamedTextColor.YELLOW);
            return;
        }
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
        dummyManager.require(args[1]);
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
        guiListener.openDummyInventory(player, dummyManager.require(args[1]));
    }

    private void tpto(CommandSender sender, String[] args) {
        requirePermission(sender, "dummy.command.tpto");
        Player player = requirePlayer(sender);
        if (args.length != 2) {
            message(sender, "usage.tpto", NamedTextColor.YELLOW);
            return;
        }
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
        dummyManager.swap(player, args[1]);
        message(sender, "command.tps-success", NamedTextColor.GREEN, args[1]);
    }

    private void actions(CommandSender sender, String[] args) {
        requirePermission(sender, "dummy.command.actions");
        if (args.length < 3) {
            message(sender, "usage.actions", NamedTextColor.YELLOW);
            return;
        }
        DummyInstance dummy = dummyManager.require(args[1]);
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

    private List<String> filter(List<String> values, String prefix) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(normalized))
                .toList();
    }

    private List<String> removeSuggestions(CommandSender sender, String prefix) {
        List<String> names = new ArrayList<>(dummyManager.names());
        if (sender.hasPermission("dummy.command.remove-all")) {
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
            if (args.length == repeatIndex + 2) {
                return filter(List.of("1", "5", "10", "20"), args[repeatIndex + 1]);
            }
            if (args.length == repeatIndex + 3) {
                return filter(List.of("20", "100", "200", "1200"), args[repeatIndex + 2]);
            }
            return List.of();
        }
        if (args[3].equalsIgnoreCase("once") && args.length == 5) {
            return filter(actionArgumentSuggestions(action), args[4]);
        }
        if (supportsMode(action)) {
            return filter(List.of("repeat"), args[args.length - 1]);
        }
        return List.of();
    }

    private boolean supportsMode(String action) {
        return MODE_ACTIONS.contains(action);
    }

    private ActionRequest parseActionRequest(String action, String[] rawArgs) {
        if (!supportsMode(action)) {
            return new ActionRequest(false, 20, -1, rawArgs);
        }
        if (rawArgs.length >= 1 && rawArgs[0].equalsIgnoreCase("once")) {
            return new ActionRequest(false, 20, -1, DummyActionService.tail(rawArgs, 1));
        }
        if (rawArgs.length >= 1 && rawArgs[0].equalsIgnoreCase("repeat")) {
            return parsePrefixRepeat(action, rawArgs);
        }

        int repeatIndex = modeIndex(rawArgs, 0, "repeat");
        if (repeatIndex >= 0) {
            return parseSuffixRepeat(rawArgs, repeatIndex);
        }
        return new ActionRequest(false, 20, -1, rawArgs);
    }

    private ActionRequest parsePrefixRepeat(String action, String[] rawArgs) {
        if (rawArgs.length <= 1) {
            throw new LocalizedException("error.repeat-requires-interval");
        }
        int interval = parseInteger(rawArgs[1]);
        int duration = -1;
        String[] actionArgs = DummyActionService.tail(rawArgs, 2);
        if (actionArgs.length == 1 && actionArgumentSuggestions(action).isEmpty() && isInteger(actionArgs[0])) {
            duration = parseInteger(actionArgs[0]);
            actionArgs = new String[0];
        }
        return new ActionRequest(true, interval, duration, actionArgs);
    }

    private ActionRequest parseSuffixRepeat(String[] rawArgs, int repeatIndex) {
        if (rawArgs.length <= repeatIndex + 1) {
            throw new LocalizedException("error.repeat-requires-interval");
        }
        int interval = parseInteger(rawArgs[repeatIndex + 1]);
        int duration = rawArgs.length > repeatIndex + 2 ? parseInteger(rawArgs[repeatIndex + 2]) : -1;
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
            return List.of("0.1", "0.25", "0.5", "1.0");
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

    private boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
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
