package dev.dummy.action;

import dev.dummy.DummyPlugin;
import dev.dummy.dummy.DummyInstance;
import dev.dummy.dummy.DummyManager;
import dev.dummy.i18n.LocalizedException;
import io.papermc.paper.entity.LookAnchor;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import net.minecraft.core.BlockPos;

public final class DummyActionService {
    private final DummyPlugin plugin;
    private final DummyManager dummyManager;
    private final Map<UUID, Map<String, BukkitTask>> tasks = new LinkedHashMap<>();
    private final Map<UUID, MineState> mineStates = new LinkedHashMap<>();

    public DummyActionService(DummyPlugin plugin, DummyManager dummyManager) {
        this.plugin = plugin;
        this.dummyManager = dummyManager;
    }

    public void run(DummyInstance dummy, String action, String[] args, boolean repeat, int intervalTicks, int durationTicks) {
        String normalized = normalize(action);
        if (normalized.equals("stop")) {
            stop(dummy, args.length == 0 ? null : args[0]);
            return;
        }

        if (!repeat) {
            perform(dummy, normalized, args);
            dummyManager.save();
            return;
        }

        stop(dummy, normalized);
        UUID uuid = dummy.uuid();
        int interval = Math.max(1, intervalTicks);
        int[] elapsed = {0};
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            DummyInstance current = dummyManager.get(uuid);
            if (current == null || isInactive(current)) {
                if (!preserveOnLifecycle()) {
                    stopTask(uuid, normalized, current);
                }
                return;
            }
            try {
                perform(current, normalized, args);
            } catch (LocalizedException ex) {
                plugin.getLogger().fine("Skipped dummy action '" + normalized + "' for " + current.name() + ": " + ex.key());
            } catch (RuntimeException ex) {
                plugin.getLogger().warning("Stopped dummy action '" + normalized + "' for " + current.name() + ": " + ex.getMessage());
                stopTask(uuid, normalized, current);
                return;
            }
            elapsed[0] += interval;
            if (durationTicks > 0 && elapsed[0] >= durationTicks) {
                stopTask(uuid, normalized, current);
            }
        }, 0L, interval);
        tasks.computeIfAbsent(uuid, ignored -> new LinkedHashMap<>()).put(normalized, task);
    }

    public int stop(DummyInstance dummy, String action) {
        Map<String, BukkitTask> dummyTasks = tasks.get(dummy.uuid());
        if (action == null || action.isBlank()) {
            int size = 0;
            if (dummyTasks != null) {
                size = dummyTasks.size();
                dummyTasks.values().forEach(BukkitTask::cancel);
                dummyTasks.clear();
            }
            tasks.remove(dummy.uuid());
            resetAll(dummy);
            return size;
        }

        String normalized = normalize(action);
        BukkitTask task = dummyTasks == null ? null : dummyTasks.remove(normalized);
        if (task != null) {
            task.cancel();
        }
        if (dummyTasks != null && dummyTasks.isEmpty()) {
            tasks.remove(dummy.uuid());
        }
        return resetAction(dummy, normalized) || task != null ? 1 : 0;
    }

    public boolean preserveOnLifecycle() {
        return plugin.getConfig().getBoolean("actions.preserve-on-lifecycle", true);
    }

    public static String[] tail(String[] args, int from) {
        if (from >= args.length) {
            return new String[0];
        }
        return Arrays.copyOfRange(args, from, args.length);
    }

    private void perform(DummyInstance dummy, String action, String[] args) {
        Player player = dummy.player();
        switch (action) {
            case "attack" -> attack(player);
            case "chat" -> player.chat(join(args));
            case "command" -> player.performCommand(stripSlash(join(args)));
            case "drop" -> player.dropItem(true);
            case "hold" -> hold(player, args);
            case "jump" -> jump(player);
            case "look" -> look(player, args);
            case "lookat" -> lookAt(player, args);
            case "mine" -> mine(player);
            case "mount" -> mount(player);
            case "move" -> move(player, args);
            case "place" -> place(player);
            case "sneak" -> player.setSneaking(parseToggle(args, player.isSneaking()));
            case "sprint" -> player.setSprinting(parseToggle(args, player.isSprinting()));
            case "swap" -> swapHands(player);
            case "use" -> use(player);
            default -> throw new LocalizedException("error.unknown-action", action);
        }
    }

    private void attack(Player player) {
        Entity target = player.getTargetEntity(4, true);
        if (target == null) {
            target = nearestEntity(player, 4.0D);
        }
        if (target == null || target.equals(player)) {
            throw new LocalizedException("error.no-target-entity");
        }
        player.attack(target);
        player.swingMainHand();
    }

    private void hold(Player player, String[] args) {
        if (args.length == 0) {
            throw new LocalizedException("error.hold-requires-slot");
        }
        int slot = Integer.parseInt(args[0]);
        if (slot < 0 || slot > 8) {
            throw new LocalizedException("error.hold-slot-range");
        }
        player.getInventory().setHeldItemSlot(slot);
    }

    private void jump(Player player) {
        Vector velocity = player.getVelocity();
        velocity.setY(Math.max(velocity.getY(), 0.42D));
        player.setVelocity(velocity);
        player.setJumping(true);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> player.setJumping(false), 2L);
    }

    private void look(Player player, String[] args) {
        if (args.length == 1) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "north" -> player.setRotation(180.0F, 0.0F);
                case "east" -> player.setRotation(-90.0F, 0.0F);
                case "south" -> player.setRotation(0.0F, 0.0F);
                case "west" -> player.setRotation(90.0F, 0.0F);
                case "entity" -> lookAtNearestEntity(player);
                default -> throw new LocalizedException("error.look-requires-rotation");
            }
            return;
        }
        if (args.length < 2) {
            throw new LocalizedException("error.look-requires-rotation");
        }
        player.setRotation(Float.parseFloat(args[0]), Float.parseFloat(args[1]));
    }

    private void lookAt(Player player, String[] args) {
        if (args.length != 3) {
            throw new LocalizedException("error.lookat-requires-coordinates");
        }
        Location location = player.getLocation();
        int x = parseBlockCoordinate(args[0], location.getBlockX());
        int y = parseBlockCoordinate(args[1], location.getBlockY());
        int z = parseBlockCoordinate(args[2], location.getBlockZ());
        player.lookAt(x + 0.5D, y + 0.5D, z + 0.5D, LookAnchor.EYES);
    }

    private void mine(Player player) {
        Block block = player.getTargetBlockExact(5, FluidCollisionMode.NEVER);
        if (block == null || block.isEmpty()) {
            mineStates.remove(player.getUniqueId());
            throw new LocalizedException("error.no-target-block");
        }

        float breakSpeed = block.getBreakSpeed(player);
        if (breakSpeed <= 0.0F) {
            mineStates.remove(player.getUniqueId());
            throw new LocalizedException("error.block-unbreakable");
        }

        UUID uuid = player.getUniqueId();
        int tick = Math.max(0, player.getTicksLived());
        MineState state = mineStates.get(uuid);
        if (state == null || !state.matches(block)) {
            state = new MineState(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ(), tick, 0.0F);
        }

        int elapsedTicks = Math.max(1, Math.min(20, tick - state.tick()));
        float progress = player.getGameMode() == GameMode.CREATIVE ? 1.0F : state.progress() + breakSpeed * elapsedTicks;
        player.swingMainHand();
        if (progress < 1.0F) {
            mineStates.put(uuid, new MineState(state.worldUuid(), state.x(), state.y(), state.z(), tick, progress));
            return;
        }

        mineStates.remove(uuid);
        if (!destroyBlock(player, block)) {
            throw new LocalizedException("error.mine-failed");
        }
        dummyManager.save();
    }

    private void mount(Player player) {
        if (player.isInsideVehicle()) {
            player.leaveVehicle();
            return;
        }
        Entity target = nearestEntity(player, plugin.getConfig().getDouble("actions.mount.range", 4.0D));
        if (target == null) {
            throw new LocalizedException("error.no-mountable-entity");
        }
        target.addPassenger(player);
    }

    private void move(Player player, String[] args) {
        double speed = args.length == 0 ? 0.25D : Double.parseDouble(args[0]);
        Vector direction = player.getLocation().getDirection().setY(0.0D);
        if (direction.lengthSquared() == 0.0D) {
            return;
        }
        player.setVelocity(direction.normalize().multiply(speed).setY(player.getVelocity().getY()));
    }

    private void place(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.isEmpty() || !held.getType().isBlock()) {
            throw new LocalizedException("error.no-placeable-block");
        }
        Block target = player.getTargetBlockExact(5, FluidCollisionMode.NEVER);
        BlockFace face = player.getTargetBlockFace(5, FluidCollisionMode.NEVER);
        if (target == null || face == null) {
            throw new LocalizedException("error.no-target-block");
        }
        if (!placeBlock(player, target, face)) {
            throw new LocalizedException("error.place-failed");
        }
        player.swingMainHand();
        dummyManager.save();
    }

    private void swapHands(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack offhand = player.getInventory().getItemInOffHand();
        player.getInventory().setItemInMainHand(offhand);
        player.getInventory().setItemInOffHand(main);
    }

    private void use(Player player) {
        player.swingMainHand();
        player.startUsingItem(EquipmentSlot.HAND);
    }

    private Entity nearestEntity(Player player, double range) {
        return player.getNearbyEntities(range, range, range)
                .stream()
                .filter(entity -> entity instanceof LivingEntity)
                .filter(entity -> !entity.equals(player))
                .min((left, right) -> Double.compare(
                        left.getLocation().distanceSquared(player.getLocation()),
                        right.getLocation().distanceSquared(player.getLocation())
                ))
                .orElse(null);
    }

    private void lookAtNearestEntity(Player player) {
        Entity target = nearestEntity(player, 8.0D);
        if (target == null) {
            throw new LocalizedException("error.no-target-entity");
        }
        player.lookAt(target, io.papermc.paper.entity.LookAnchor.EYES, io.papermc.paper.entity.LookAnchor.EYES);
    }

    private boolean parseToggle(String[] args, boolean currentValue) {
        if (args.length > 1) {
            throw new LocalizedException("error.toggle-argument");
        }
        if (args.length == 0) {
            return !currentValue;
        }
        String value = args[0].toLowerCase(Locale.ROOT);
        if (value.equals("toggle")) {
            return !currentValue;
        }
        if (value.equals("on")) {
            return true;
        }
        if (value.equals("off")) {
            return false;
        }
        throw new LocalizedException("error.toggle-argument");
    }

    private void resetAll(DummyInstance dummy) {
        for (String action : java.util.List.of("jump", "move", "mine", "sneak", "sprint", "mount", "use")) {
            resetAction(dummy, action);
        }
    }

    private boolean resetAction(DummyInstance dummy, String action) {
        Player player = dummy.player();
        switch (action) {
            case "jump" -> player.setJumping(false);
            case "move" -> player.setVelocity(player.getVelocity().setX(0.0D).setZ(0.0D));
            case "mine" -> mineStates.remove(player.getUniqueId());
            case "sneak" -> player.setSneaking(false);
            case "sprint" -> player.setSprinting(false);
            case "mount" -> player.leaveVehicle();
            case "use" -> player.clearActiveItem();
            default -> {
                return false;
            }
        }
        dummyManager.save();
        return true;
    }

    private void stopTask(UUID uuid, String action, DummyInstance dummy) {
        Map<String, BukkitTask> dummyTasks = tasks.get(uuid);
        BukkitTask task = dummyTasks == null ? null : dummyTasks.remove(action);
        if (task != null) {
            task.cancel();
        }
        if (dummyTasks != null && dummyTasks.isEmpty()) {
            tasks.remove(uuid);
        }
        if (dummy != null) {
            resetAction(dummy, action);
        } else if (action.equals("mine")) {
            mineStates.remove(uuid);
        }
    }

    private boolean isInactive(DummyInstance dummy) {
        Player player = dummy.player();
        return player.isDead() || !player.isValid();
    }

    private boolean destroyBlock(Player player, Block block) {
        if (player instanceof CraftPlayer craftPlayer) {
            return craftPlayer.getHandle().gameMode.destroyBlock(new BlockPos(block.getX(), block.getY(), block.getZ()));
        }
        return player.breakBlock(block);
    }

    private boolean placeBlock(Player player, Block target, BlockFace face) {
        if (!(player instanceof CraftPlayer craftPlayer)) {
            return false;
        }
        ServerPlayer serverPlayer = craftPlayer.getHandle();
        InteractionHand hand = InteractionHand.MAIN_HAND;
        net.minecraft.world.item.ItemStack stack = serverPlayer.getItemInHand(hand);
        Direction direction = CraftBlock.blockFaceToNotch(face);
        BlockHitResult hit = new BlockHitResult(
                new Vec3(
                        target.getX() + 0.5D + face.getModX() * 0.5D,
                        target.getY() + 0.5D + face.getModY() * 0.5D,
                        target.getZ() + 0.5D + face.getModZ() * 0.5D
                ),
                direction,
                new BlockPos(target.getX(), target.getY(), target.getZ()),
                false
        );
        InteractionResult result = serverPlayer.gameMode.useItemOn(serverPlayer, serverPlayer.level(), stack, hand, hit);
        return result.consumesAction();
    }

    private int parseBlockCoordinate(String raw, int base) {
        try {
            if (raw.equals("~")) {
                return base;
            }
            if (raw.startsWith("~")) {
                return base + Integer.parseInt(raw.substring(1));
            }
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            throw new LocalizedException("error.invalid-coordinate", raw);
        }
    }

    private String join(String[] args) {
        if (args.length == 0) {
            throw new LocalizedException("error.action-requires-arguments");
        }
        return String.join(" ", args);
    }

    private String stripSlash(String command) {
        return command.startsWith("/") ? command.substring(1) : command;
    }

    private String normalize(String action) {
        return action.toLowerCase(Locale.ROOT);
    }

    private record MineState(UUID worldUuid, int x, int y, int z, int tick, float progress) {
        private boolean matches(Block block) {
            return block.getWorld().getUID().equals(worldUuid)
                    && block.getX() == x
                    && block.getY() == y
                    && block.getZ() == z;
        }
    }
}
