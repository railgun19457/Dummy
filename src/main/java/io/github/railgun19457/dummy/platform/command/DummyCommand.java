package io.github.railgun19457.dummy.platform.command;

import io.github.railgun19457.dummy.core.action.ActionRequest;
import io.github.railgun19457.dummy.core.action.ActionResult;
import io.github.railgun19457.dummy.core.action.ActionType;
import io.github.railgun19457.dummy.core.model.DummyDefinition;
import io.github.railgun19457.dummy.core.model.DummyId;
import io.github.railgun19457.dummy.core.model.Rotation;
import io.github.railgun19457.dummy.core.model.WorldPosition;
import io.github.railgun19457.dummy.core.port.MessageGateway;
import io.github.railgun19457.dummy.core.service.DummyControlService;
import io.github.railgun19457.dummy.core.service.DummyLifecycleService;
import io.github.railgun19457.dummy.core.service.DummyOwnershipService;
import io.github.railgun19457.dummy.core.service.DummyQueryService;
import io.github.railgun19457.dummy.platform.gui.DummyInventoryView;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class DummyCommand implements CommandExecutor, TabCompleter {

    private final DummyLifecycleService lifecycleService;
    private final DummyControlService controlService;
    private final DummyQueryService queryService;
    private final DummyOwnershipService ownershipService;
    private final MessageGateway messageGateway;

    public DummyCommand(
            DummyLifecycleService lifecycleService,
            DummyControlService controlService,
            DummyQueryService queryService,
            DummyOwnershipService ownershipService,
            MessageGateway messageGateway
    ) {
        this.lifecycleService = lifecycleService;
        this.controlService = controlService;
        this.queryService = queryService;
        this.ownershipService = ownershipService;
        this.messageGateway = messageGateway;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            messageGateway.sendMessage(sender, "dummy.command.usage.root");
            return true;
        }
        String subCommand = args[0].toLowerCase(Locale.ROOT);
        try {
            return switch (subCommand) {
                case "spawn" -> handleSpawn(sender, args);
                case "remove" -> handleRemove(sender, args);
                case "list" -> handleList(sender, args);
                case "tp" -> handleTeleport(sender, args);
                case "swap" -> handleSwap(sender, args);
                case "skin" -> handleSkin(sender, args);
                case "inv" -> handleInventory(sender, args);
                case "exp" -> handleExp(sender, args);
                case "config" -> handleConfig(sender, args);
                case "action" -> handleAction(sender, args);
                default -> {
                    messageGateway.sendMessage(sender, "dummy.command.error.unknown-subcommand", Map.of("subcommand", subCommand));
                    yield true;
                }
            };
        } catch (IllegalArgumentException | IllegalStateException exception) {
            sender.sendMessage(exception.getMessage());
            return true;
        }
    }

    private boolean handleSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messageGateway.sendMessage(sender, "dummy.command.error.player-only");
            return true;
        }
        String requestedName = args.length >= 2 ? args[1] : null;
        lifecycleService.spawn(player, requestedName).thenAccept(definition -> messageGateway.sendMessage(
                sender,
                "dummy.spawn.success",
                Map.of("name", definition.name(), "id", definition.id().toString())
        ));
        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            messageGateway.sendMessage(sender, "dummy.command.usage.remove");
            return true;
        }
        DummyDefinition definition = requireAccessibleDefinition(sender, args[1], "dummy.remove", "dummy.remove.others");
        messageGateway.sendMessage(sender, lifecycleService.remove(definition.id()) ? "dummy.remove.success" : "dummy.error.dummy-not-found");
        return true;
    }

    private boolean handleList(CommandSender sender, String[] args) {
        List<DummyDefinition> definitions;
        if (args.length >= 2) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                messageGateway.sendMessage(sender, "dummy.error.player-not-found", Map.of("player", args[1]));
                return true;
            }
            if (!(sender instanceof Player player) || !player.getUniqueId().equals(target.getUniqueId())) {
                if (!sender.hasPermission("dummy.list.others") && !sender.hasPermission("dummy.admin")) {
                    messageGateway.sendMessage(sender, "dummy.permission.denied");
                    return true;
                }
            }
            definitions = queryService.findByOwner(target.getUniqueId());
        } else if (sender instanceof Player player) {
            definitions = queryService.findByOwner(player.getUniqueId());
        } else {
            definitions = queryService.findAll();
        }
        if (definitions.isEmpty()) {
            messageGateway.sendMessage(sender, "dummy.list.empty");
            return true;
        }
        messageGateway.sendMessage(sender, "dummy.list.header");
        definitions.forEach(definition -> sender.sendMessage(messageGateway.resolve("dummy.list.entry", Map.of(
                "name", definition.name(),
                "id", definition.id().toString()
        ))));
        return true;
    }

    private boolean handleTeleport(CommandSender sender, String[] args) {
        if (args.length < 3) {
            messageGateway.sendMessage(sender, "dummy.command.usage.tp");
            return true;
        }
        DummyDefinition definition = requireAccessibleDefinition(sender, args[1], "dummy.teleport", "dummy.teleport.others");
        WorldPosition worldPosition;
        if (args.length == 3) {
            Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                messageGateway.sendMessage(sender, "dummy.error.player-not-found", Map.of("player", args[2]));
                return true;
            }
            Location location = target.getLocation();
            worldPosition = new WorldPosition(location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), new Rotation(location.getYaw(), location.getPitch()));
        } else if (args.length >= 5) {
            if (!(sender instanceof Player player)) {
                messageGateway.sendMessage(sender, "dummy.command.error.console-target-required");
                return true;
            }
            worldPosition = new WorldPosition(
                    player.getWorld().getName(),
                    Double.parseDouble(args[2]),
                    Double.parseDouble(args[3]),
                    Double.parseDouble(args[4]),
                    new Rotation(player.getLocation().getYaw(), player.getLocation().getPitch())
            );
        } else {
            messageGateway.sendMessage(sender, "dummy.command.usage.tp");
            return true;
        }
        lifecycleService.teleport(definition.id(), worldPosition);
        messageGateway.sendMessage(sender, "dummy.teleport.success");
        return true;
    }

    private boolean handleSkin(CommandSender sender, String[] args) {
        if (args.length < 2) {
            messageGateway.sendMessage(sender, "dummy.command.usage.skin");
            return true;
        }
        DummyDefinition definition = requireAccessibleDefinition(sender, args[1], "dummy.skin", "dummy.skin.others");
        String skinOwner = args.length >= 3 ? args[2] : definition.ownerName();
        lifecycleService.updateSkin(definition.id(), skinOwner);
        messageGateway.sendMessage(sender, "dummy.skin.success", Map.of("player", skinOwner));
        return true;
    }

    private boolean handleInventory(CommandSender sender, String[] args) {
        if (args.length < 2) {
            messageGateway.sendMessage(sender, "dummy.command.usage.inv");
            return true;
        }
        DummyDefinition definition = requireAccessibleDefinition(sender, args[1], "dummy.inventory", "dummy.inventory.others");
        if (sender instanceof Player player) {
            DummyInventoryView.open(player, definition);
            return true;
        }
        messageGateway.sendMessage(sender, "dummy.inventory.summary", Map.of(
                "count", String.valueOf(definition.inventoryState().contents().size())
        ));
        return true;
    }

    private boolean handleSwap(CommandSender sender, String[] args) {
        if (args.length < 3) {
            messageGateway.sendMessage(sender, "dummy.command.usage.swap");
            return true;
        }
        DummyDefinition definition = requireAccessibleDefinition(sender, args[1], "dummy.teleport", "dummy.teleport.others");
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            messageGateway.sendMessage(sender, "dummy.error.player-not-found", Map.of("player", args[2]));
            return true;
        }

        World world = Bukkit.getWorld(definition.lastKnownPosition().worldName());
        if (world == null) {
            messageGateway.sendMessage(sender, "dummy.error.world-not-found", Map.of("world", definition.lastKnownPosition().worldName()));
            return true;
        }

        Location targetLocation = target.getLocation();
        Location dummyLocation = new Location(
                world,
                definition.lastKnownPosition().x(),
                definition.lastKnownPosition().y(),
                definition.lastKnownPosition().z(),
                definition.lastKnownPosition().rotation().yaw(),
                definition.lastKnownPosition().rotation().pitch()
        );

        lifecycleService.teleport(definition.id(), new WorldPosition(
                targetLocation.getWorld().getName(),
                targetLocation.getX(),
                targetLocation.getY(),
                targetLocation.getZ(),
                new Rotation(targetLocation.getYaw(), targetLocation.getPitch())
        ));
        target.teleport(dummyLocation);
        messageGateway.sendMessage(sender, "dummy.swap.success", Map.of("player", target.getName()));
        return true;
    }

    private boolean handleExp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messageGateway.sendMessage(sender, "dummy.command.error.player-only");
            return true;
        }
        if (args.length < 3 || !"transfer".equalsIgnoreCase(args[2])) {
            messageGateway.sendMessage(sender, "dummy.command.usage.exp-transfer");
            return true;
        }
        DummyDefinition definition = requireAccessibleDefinition(sender, args[1], "dummy.exp", "dummy.exp.others");
        int amount = lifecycleService.transferExperience(definition.id(), player);
        messageGateway.sendMessage(sender, amount > 0 ? "dummy.exp.transfer.success" : "dummy.exp.transfer.empty", Map.of("amount", String.valueOf(amount)));
        return true;
    }

    private boolean handleConfig(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messageGateway.sendMessage(sender, "dummy.command.error.player-only");
            return true;
        }
        if (args.length < 2) {
            messageGateway.sendMessage(sender, "dummy.command.usage.config");
            return true;
        }
        DummyDefinition definition = requireAccessibleDefinition(sender, args[1], "dummy.config", "dummy.config.others");
        DummyInventoryView.openConfig(player, definition);
        return true;
    }

    private boolean handleAction(CommandSender sender, String[] args) {
        if (args.length < 3) {
            messageGateway.sendMessage(sender, "dummy.command.usage.action");
            return true;
        }
        DummyDefinition definition = requireAccessibleDefinition(sender, args[1], "dummy.control", "dummy.control.others");
        ActionType actionType = parseActionType(args[2]);
        Map<String, Object> actionArguments = parseActionArguments(actionType, args);
        ActionResult result = controlService.execute(actionType, new ActionRequest(definition.id(), sender, actionArguments));
        messageGateway.sendMessage(sender, result.messageKey());
        return true;
    }

    private DummyDefinition requireAccessibleDefinition(CommandSender sender, String token, String ownPermission, String othersPermission) {
        DummyDefinition definition = resolveDefinition(token)
                .orElseThrow(() -> new IllegalArgumentException(messageGateway.resolve("dummy.error.dummy-not-found", Map.of("dummy", token))));
        if (!ownershipService.canAccess(sender, definition, ownPermission, othersPermission)) {
            throw new IllegalStateException(messageGateway.resolve("dummy.permission.denied", Map.of()));
        }
        return definition;
    }

    private Optional<DummyDefinition> resolveDefinition(String token) {
        try {
            return queryService.findById(new DummyId(UUID.fromString(token)));
        } catch (IllegalArgumentException ignored) {
            return queryService.findAll().stream()
                    .filter(definition -> definition.name().equalsIgnoreCase(token))
                    .findFirst();
        }
    }

    private ActionType parseActionType(String token) {
        String normalized = token.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        if ("SWAPHAND".equals(normalized)) {
            normalized = "SWAP_HAND";
        }
        try {
            return ActionType.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(messageGateway.resolve("dummy.action.unsupported", Map.of()));
        }
    }

    private Map<String, Object> parseActionArguments(ActionType actionType, String[] args) {
        try {
            return switch (actionType) {
            case USE, JUMP, SWAP_HAND, DROP, WAKE -> Map.of();
            case SNEAK, RUN -> Map.of("enabled", args.length >= 4 ? Boolean.parseBoolean(args[3]) : Boolean.TRUE);
            case LOOK -> {
                if (args.length < 5) {
                    throw new IllegalArgumentException(messageGateway.resolve("dummy.command.usage.action-look", Map.of()));
                }
                yield Map.of(
                        "yaw", Float.parseFloat(args[3]),
                        "pitch", Float.parseFloat(args[4])
                );
            }
            case ATTACK, LOOK_AT_ENTITY, RIDE -> {
                if (args.length < 4) {
                    throw new IllegalArgumentException(messageGateway.resolve("dummy.command.usage.action-target", Map.of()));
                }
                yield Map.of("target", args[3]);
            }
            case HOLD_ITEM -> {
                if (args.length < 4) {
                    throw new IllegalArgumentException(messageGateway.resolve("dummy.command.usage.action-hold-item", Map.of()));
                }
                Material material = Material.matchMaterial(args[3]);
                if (material == null) {
                    throw new IllegalArgumentException(messageGateway.resolve("dummy.action.invalid-args", Map.of()));
                }
                yield Map.of(
                        "material", material,
                        "amount", args.length >= 5 ? Integer.parseInt(args[4]) : 1
                );
            }
            case MOVE -> {
                if (args.length < 6) {
                    throw new IllegalArgumentException(messageGateway.resolve("dummy.command.usage.action-move", Map.of()));
                }
                yield Map.of(
                        "x", Double.parseDouble(args[3]),
                        "y", Double.parseDouble(args[4]),
                        "z", Double.parseDouble(args[5]),
                        "yaw", args.length >= 7 ? Float.parseFloat(args[6]) : 0.0F,
                        "pitch", args.length >= 8 ? Float.parseFloat(args[7]) : 0.0F
                );
            }
            case DIG -> {
                if (args.length < 6) {
                    throw new IllegalArgumentException(messageGateway.resolve("dummy.command.usage.action-dig", Map.of()));
                }
                yield Map.of(
                        "x", Integer.parseInt(args[3]),
                        "y", Integer.parseInt(args[4]),
                        "z", Integer.parseInt(args[5])
                );
            }
            case SLEEP -> Map.of();
            case SEND_CHAT -> {
                if (args.length < 4) {
                    throw new IllegalArgumentException(messageGateway.resolve("dummy.command.usage.action-chat", Map.of()));
                }
                yield Map.of("message", String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length)));
            }
            case EXECUTE_COMMAND -> {
                if (args.length < 4) {
                    throw new IllegalArgumentException(messageGateway.resolve("dummy.command.usage.action-command", Map.of()));
                }
                yield Map.of("command", String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length)));
            }
            default -> throw new IllegalArgumentException(messageGateway.resolve("dummy.action.unsupported", Map.of()));
            };
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(messageGateway.resolve("dummy.action.invalid-args", Map.of()));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("spawn", "remove", "list", "tp", "swap", "skin", "inv", "exp", "config", "action");
        }
        if (args.length == 2 && List.of("remove", "tp", "swap", "skin", "inv", "exp", "config", "action").contains(args[0].toLowerCase(Locale.ROOT))) {
            List<String> suggestions = new ArrayList<>();
            for (DummyDefinition definition : queryService.findAll()) {
                suggestions.add(definition.name());
            }
            return suggestions;
        }
        if (args.length == 3 && "exp".equalsIgnoreCase(args[0])) {
            return List.of("transfer");
        }
        if (args.length == 3 && "swap".equalsIgnoreCase(args[0])) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (args.length == 3 && "action".equalsIgnoreCase(args[0])) {
            return List.of("drop", "use", "sleep", "wake", "attack", "dig", "jump", "look", "look_at_entity", "move", "run", "sneak", "ride", "swap_hand", "hold_item", "execute_command", "send_chat");
        }
        if (args.length == 4 && "action".equalsIgnoreCase(args[0]) && List.of("attack", "look_at_entity", "ride").contains(args[2].toLowerCase(Locale.ROOT))) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (args.length == 4 && "action".equalsIgnoreCase(args[0]) && "hold_item".equalsIgnoreCase(args[2])) {
            return List.of("STONE", "DIAMOND_SWORD", "SHIELD", "BREAD", "TORCH");
        }
        return List.of();
    }
}
