package dev.dummy.action;

import dev.dummy.DummyPlugin;
import dev.dummy.dummy.DummyInstance;
import dev.dummy.dummy.DummyManager;
import dev.dummy.i18n.LocalizedException;
import io.papermc.paper.entity.LookAnchor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
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
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import net.minecraft.core.BlockPos;

public final class DummyActionService {
    private static final double NORMAL_WALK_SPEED = 0.215D;
    private static final double NORMAL_SPRINT_SPEED = 0.280D;
    private static final int NORMAL_JUMP_INTERVAL_TICKS = 12;
    private static final double ATTACK_RANGE = 4.0D;
    private static final double LOOK_ENTITY_RANGE = 8.0D;
    private static final int LOCK_OBSCURED_TIMEOUT_TICKS = 40;
    private static final float LOOK_ENTITY_MAX_YAW_STEP = 18.0F;
    private static final float LOOK_ENTITY_MAX_PITCH_STEP = 12.0F;

    private final DummyPlugin plugin;
    private final DummyManager dummyManager;
    private final Map<UUID, Map<String, ActiveAction>> tasks = new LinkedHashMap<>();
    private final Map<UUID, MineState> mineStates = new LinkedHashMap<>();
    private final Map<UUID, LockedTarget> lockedTargets = new LinkedHashMap<>();

    public DummyActionService(DummyPlugin plugin, DummyManager dummyManager) {
        this.plugin = plugin;
        this.dummyManager = dummyManager;
    }

    /**
     * Re-apply a set of previously persisted repeat actions (e.g. after a
     * plugin restart). Each command string is the raw user input that
     * originally followed {@code /dummy <name> actions ...} — i.e. the
     * action keyword plus its arguments and optional {@code repeat} clause.
     * Unknown or malformed commands are skipped with a warning.
     */
    public void restoreActions(DummyInstance dummy, List<String> commands) {
        if (commands == null || commands.isEmpty()) {
            return;
        }
        for (String command : commands) {
            if (command == null || command.isBlank()) {
                continue;
            }
            try {
                String[] parts = command.trim().split("\\s+");
                if (parts.length == 0) {
                    continue;
                }
                String action = parts[0].toLowerCase(Locale.ROOT);
                String[] args = java.util.Arrays.copyOfRange(parts, 1, parts.length);
                boolean repeat = false;
                int interval = DummyActionService.defaultRepeatInterval(action);
                int duration = -1;
                int repeatIndex = -1;
                for (int i = 0; i < args.length; i++) {
                    if (args[i].equalsIgnoreCase("repeat")) {
                        repeat = true;
                        repeatIndex = i;
                        break;
                    }
                }
                if (repeat) {
                    for (int i = repeatIndex + 1; i < args.length; i++) {
                        String opt = args[i];
                        if (opt.toLowerCase(Locale.ROOT).startsWith("interval:")) {
                            try {
                                interval = Math.max(1, Integer.parseInt(opt.substring("interval:".length())));
                            } catch (NumberFormatException ignored) { }
                        } else if (opt.toLowerCase(Locale.ROOT).startsWith("duration:")) {
                            try {
                                duration = Math.max(0, Integer.parseInt(opt.substring("duration:".length())));
                            } catch (NumberFormatException ignored) { }
                        }
                    }
                    String[] actionArgs = java.util.Arrays.copyOfRange(args, 0, repeatIndex);
                    run(dummy, action, actionArgs, true, interval, duration);
                } else {
                    // Persisted as repeat (per schema) but command lacked the
                    // 'repeat' keyword — restore as repeat with default interval.
                    run(dummy, action, args, true, interval, duration);
                }
            } catch (RuntimeException ex) {
                plugin.getLogger().warning("Failed to restore action '" + command + "' for dummy " + dummy.name() + ": " + ex.getMessage());
            }
        }
    }

    /**
     * Snapshot the repeat actions currently scheduled for a dummy so they
     * can be persisted and later replayed via {@link #restoreActions}.
     * Returns an empty list for dummies with no scheduled actions.
     */
    public List<String> snapshotActions(UUID dummyUuid) {
        Map<String, ActiveAction> dummyTasks = tasks.get(dummyUuid);
        if (dummyTasks == null || dummyTasks.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>(dummyTasks.size());
        for (ActiveAction active : dummyTasks.values()) {
            result.add(active.toCommand());
        }
        return result;
    }

    public void run(DummyInstance dummy, String action, String[] args, boolean repeat, int intervalTicks, int durationTicks) {
        String normalized = normalize(action);
        if (normalized.equals("stop")) {
            stop(dummy, args.length == 0 ? null : args[0]);
            return;
        }

        if (!repeat) {
            perform(dummy, normalized, args, false);
            dummyManager.markDirty(dummy.name());
            return;
        }

        stop(dummy, normalized);
        UUID uuid = dummy.uuid();
        int interval = Math.max(minimumRepeatInterval(normalized), intervalTicks);
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
                perform(current, normalized, args, true);
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
        tasks.computeIfAbsent(uuid, ignored -> new LinkedHashMap<>()).put(normalized, new ActiveAction(task, normalized, args, interval, durationTicks));
        dummyManager.markDirty(dummy.name());
    }

    public int stop(DummyInstance dummy, String action) {
        Map<String, ActiveAction> dummyTasks = tasks.get(dummy.uuid());
        if (action == null || action.isBlank()) {
            int size = 0;
            if (dummyTasks != null) {
                size = dummyTasks.size();
                dummyTasks.values().forEach(ActiveAction::cancel);
                dummyTasks.clear();
            }
            tasks.remove(dummy.uuid());
            resetAll(dummy);
            if (size > 0) {
                dummyManager.markDirty(dummy.name());
            }
            return size;
        }

        String normalized = normalize(action);
        ActiveAction active = dummyTasks == null ? null : dummyTasks.remove(normalized);
        if (active != null) {
            active.cancel();
        }
        if (dummyTasks != null && dummyTasks.isEmpty()) {
            tasks.remove(dummy.uuid());
        }
        boolean reset = resetAction(dummy, normalized);
        if (active != null || reset) {
            dummyManager.markDirty(dummy.name());
        }
        return active != null || reset ? 1 : 0;
    }

    public List<String> activeActions(DummyInstance dummy) {
        Map<String, ActiveAction> dummyTasks = tasks.get(dummy.uuid());
        if (dummyTasks == null || dummyTasks.isEmpty()) {
            return List.of();
        }
        return List.copyOf(dummyTasks.keySet());
    }

    public boolean preserveOnLifecycle() {
        return plugin.getConfig().getBoolean("actions.preserve-on-lifecycle", true);
    }

    private boolean attackAutoTargetsNearestVisible() {
        return plugin.getConfig().getBoolean("actions.attack.auto-target-nearest-visible", false);
    }

    public static String[] tail(String[] args, int from) {
        if (from >= args.length) {
            return new String[0];
        }
        return Arrays.copyOfRange(args, from, args.length);
    }

    public static int defaultRepeatInterval(String action) {
        return switch (action.toLowerCase(Locale.ROOT)) {
            case "look", "mine", "move" -> 1;
            case "jump" -> NORMAL_JUMP_INTERVAL_TICKS;
            default -> 20;
        };
    }

    public static int minimumRepeatInterval(String action) {
        return action.equalsIgnoreCase("jump") ? NORMAL_JUMP_INTERVAL_TICKS : 1;
    }

    private void perform(DummyInstance dummy, String action, String[] args, boolean repeated) {
        Player player = dummy.player();
        switch (action) {
            case "attack" -> attack(player);
            case "chat" -> player.chat(join(args));
            case "command" -> player.performCommand(stripSlash(join(args)));
            case "drop" -> player.dropItem(true);
            case "hold" -> hold(player, args);
            case "jump" -> jump(player);
            case "look" -> look(player, args, repeated);
            case "lookat" -> lookAt(player, args);
            case "mine" -> mine(player);
            case "mount" -> mount(player);
            case "move" -> move(player, args);
            case "place" -> place(player);
            case "sneak" -> player.setSneaking(parseToggle(args, player.isSneaking()));
            case "swap" -> swapHands(player);
            case "use" -> use(player);
            default -> throw new LocalizedException("error.unknown-action", action);
        }
    }

    private void attack(Player player) {
        Entity target = resolveAttackTarget(player);
        if (target == null) {
            throw new LocalizedException("error.no-target-entity");
        }
        if (player.getAttackCooldown() < 1.0F) {
            player.swingMainHand();
            throw new LocalizedException("error.attack-cooldown");
        }
        lookAtEntity(player, target, false);
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
        if (!player.isOnGround()) {
            return;
        }
        Vector velocity = player.getVelocity();
        velocity.setY(Math.max(velocity.getY(), 0.42D));
        player.setVelocity(velocity);
        player.setJumping(true);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> player.setJumping(false), 2L);
    }

    private void look(Player player, String[] args, boolean repeated) {
        if (args.length == 0) {
            throw new LocalizedException("error.look-requires-rotation");
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "direction" -> lookDirection(player, args);
            case "entity" -> lookAtEntityTarget(player, args, repeated);
            case "angle" -> lookAngle(player, args);
            default -> throw new LocalizedException("error.look-requires-rotation");
        }
    }

    private void lookDirection(Player player, String[] args) {
        if (args.length != 2) {
            throw new LocalizedException("error.look-requires-rotation");
        }
        lockedTargets.remove(player.getUniqueId());
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "north" -> player.setRotation(180.0F, 0.0F);
            case "east" -> player.setRotation(-90.0F, 0.0F);
            case "south" -> player.setRotation(0.0F, 0.0F);
            case "west" -> player.setRotation(90.0F, 0.0F);
            default -> throw new LocalizedException("error.look-requires-rotation");
        }
    }

    private void lookAngle(Player player, String[] args) {
        if (args.length != 3) {
            throw new LocalizedException("error.look-requires-rotation");
        }
        lockedTargets.remove(player.getUniqueId());
        Location location = player.getLocation();
        float yaw = parseRotationCoordinate(args[1], location.getYaw());
        float pitch = parseRotationCoordinate(args[2], location.getPitch());
        player.setRotation(normalizeYaw(yaw), clampPitch(pitch));
    }

    private void lookAtEntityTarget(Player player, String[] args, boolean smooth) {
        if (args.length > 3) {
            throw new LocalizedException("error.look-requires-rotation");
        }

        LookTargetType targetType = LookTargetType.ANY;
        String targetName = null;
        if (args.length == 1) {
            targetType = LookTargetType.ANY;
        } else {
            switch (args[1].toLowerCase(Locale.ROOT)) {
                case "player" -> {
                    targetType = LookTargetType.PLAYER;
                    if (args.length == 3) {
                        targetName = args[2];
                    }
                }
                case "monster" -> {
                    if (args.length != 2) {
                        throw new LocalizedException("error.look-requires-rotation");
                    }
                    targetType = LookTargetType.MONSTER;
                }
                default -> throw new LocalizedException("error.look-requires-rotation");
            }
        }

        Entity target = resolveLookTarget(player, targetType, targetName);
        if (target == null) {
            throw new LocalizedException("error.no-target-entity");
        }
        lookAtEntity(player, target, smooth);
    }

    private void lookAt(Player player, String[] args) {
        if (args.length != 3) {
            throw new LocalizedException("error.lookat-requires-coordinates");
        }
        lockedTargets.remove(player.getUniqueId());
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
        player.swingMainHand();
        dummyManager.markDirty(player.getName());
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
        if (target instanceof Vehicle) {
            target.setPersistent(true);
        }
        target.addPassenger(player);
    }

    private void move(Player player, String[] args) {
        if (args.length > 1) {
            throw new LocalizedException("error.move-speed-argument");
        }
        double speed = (args.length == 0 ? NORMAL_WALK_SPEED : parseMoveSpeed(args[0])) * movementPotionMultiplier(player);
        Vector direction = player.getLocation().getDirection().setY(0.0D);
        if (direction.lengthSquared() == 0.0D) {
            return;
        }
        player.setVelocity(direction.normalize().multiply(speed).setY(player.getVelocity().getY()));
    }

    private double parseMoveSpeed(String raw) {
        String normalized = raw.toLowerCase(Locale.ROOT);
        if (normalized.equals("slow")) {
            return 0.1D;
        }
        if (normalized.equals("walk")) {
            return NORMAL_WALK_SPEED;
        }
        if (normalized.equals("sprint")) {
            return NORMAL_SPRINT_SPEED;
        }
        double speed;
        try {
            speed = Double.parseDouble(raw);
        } catch (NumberFormatException ignored) {
            throw new LocalizedException("error.invalid-number", raw);
        }
        if (speed < 0.0D || speed > NORMAL_SPRINT_SPEED) {
            throw new LocalizedException("error.move-speed-range", String.format(Locale.ROOT, "%.3f", NORMAL_SPRINT_SPEED));
        }
        return speed;
    }

    private double movementPotionMultiplier(Player player) {
        double multiplier = 1.0D;
        PotionEffect speed = player.getPotionEffect(PotionEffectType.SPEED);
        if (speed != null) {
            multiplier *= 1.0D + 0.2D * (speed.getAmplifier() + 1);
        }
        PotionEffect slowness = player.getPotionEffect(PotionEffectType.SLOWNESS);
        if (slowness != null) {
            multiplier *= Math.max(0.0D, 1.0D - 0.15D * (slowness.getAmplifier() + 1));
        }
        return multiplier;
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
        dummyManager.markDirty(player.getName());
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
                .filter(entity -> !entity.equals(player))
                .filter(DummyActionService::isMountable)
                .min((left, right) -> Double.compare(
                        left.getLocation().distanceSquared(player.getLocation()),
                        right.getLocation().distanceSquared(player.getLocation())
                ))
                .orElse(null);
    }

    /**
     * Whether an entity can be ridden by a dummy player. The previous
     * {@code LivingEntity}-only filter excluded boats and minecarts (which
     * implement {@link Vehicle} but not {@link LivingEntity}), so they are
     * matched explicitly here.
     */
    private static boolean isMountable(Entity entity) {
        return entity instanceof LivingEntity
                || entity instanceof Boat
                || entity instanceof Minecart;
    }

    /**
     * Whether a non-living entity can be selected as a look/attack target.
     * Mirrors {@link #isMountable(Entity)}: boats and minecarts are the only
     * non-living entities currently worth tracking for dummy gaze/aim.
     */
    private static boolean isLookable(Entity entity) {
        return entity instanceof Boat || entity instanceof Minecart;
    }

    private Entity resolveLookTarget(Player player, LookTargetType targetType, String targetName) {
        TargetResolution locked = lockedTarget(player, targetType, targetName, LOOK_ENTITY_RANGE);
        if (locked.target() != null || locked.waitingForLockedTarget()) {
            return locked.target();
        }

        Entity target = targetName == null
                ? nearestVisibleTarget(player, targetType, LOOK_ENTITY_RANGE)
                : namedPlayerTarget(player, targetName, LOOK_ENTITY_RANGE);
        if (target != null) {
            lockTarget(player, target, targetType == LookTargetType.ANY ? targetTypeFor(target) : targetType, targetName);
        }
        return target;
    }

    private Entity resolveAttackTarget(Player player) {
        TargetResolution locked = lockedTarget(player, LookTargetType.ANY, null, ATTACK_RANGE);
        if (locked.target() != null || locked.waitingForLockedTarget()) {
            return locked.target();
        }

        Entity target = player.getTargetEntity((int) Math.ceil(ATTACK_RANGE), false);
        if (!isTargetCandidate(player, target, LookTargetType.ANY, ATTACK_RANGE, true)) {
            target = attackAutoTargetsNearestVisible()
                    ? nearestVisibleTarget(player, LookTargetType.ANY, ATTACK_RANGE)
                    : null;
        }
        if (target != null) {
            lockTarget(player, target, targetTypeFor(target), null);
        }
        return target;
    }

    private TargetResolution lockedTarget(Player player, LookTargetType targetType, String targetName, double range) {
        UUID playerUuid = player.getUniqueId();
        LockedTarget locked = lockedTargets.get(playerUuid);
        if (locked == null || !locked.matches(targetType, targetName)) {
            return TargetResolution.none();
        }

        Entity target = plugin.getServer().getEntity(locked.targetUuid());
        if (!isTargetCandidate(player, target, locked.targetType(), range, false)) {
            lockedTargets.remove(playerUuid);
            return TargetResolution.none();
        }
        if (!player.hasLineOfSight(target)) {
            int tick = Math.max(0, player.getTicksLived());
            int obscuredSinceTick = locked.obscuredSinceTick() < 0 ? tick : locked.obscuredSinceTick();
            if (tick - obscuredSinceTick >= LOCK_OBSCURED_TIMEOUT_TICKS) {
                lockedTargets.remove(playerUuid);
                return TargetResolution.none();
            }
            lockedTargets.put(playerUuid, locked.withObscuredSinceTick(obscuredSinceTick));
            return TargetResolution.waiting();
        }

        if (locked.obscuredSinceTick() >= 0) {
            lockedTargets.put(playerUuid, locked.withObscuredSinceTick(-1));
        }
        return new TargetResolution(target, false);
    }

    private Entity nearestVisibleTarget(Player player, LookTargetType targetType, double range) {
        return player.getNearbyEntities(range, range, range)
                .stream()
                .filter(entity -> isTargetCandidate(player, entity, targetType, range, true))
                .min((left, right) -> Double.compare(
                        left.getLocation().distanceSquared(player.getLocation()),
                        right.getLocation().distanceSquared(player.getLocation())
                ))
                .orElse(null);
    }

    private Player namedPlayerTarget(Player player, String name, double range) {
        Player target = plugin.getServer().getPlayerExact(name);
        if (!isTargetCandidate(player, target, LookTargetType.PLAYER, range, true)) {
            return null;
        }
        return target;
    }

    private boolean isTargetCandidate(Player player, Entity entity, LookTargetType targetType, double range, boolean requireLineOfSight) {
        if (entity == null || entity.equals(player) || !entity.isValid()) {
            return false;
        }
        if (entity instanceof LivingEntity living && living.isDead()) {
            return false;
        }
        // Non-living entities (boats, minecarts, etc.) are valid look/attack
        // targets only when explicitly allowed by isLookable, since the previous
        // LivingEntity-only filter silently dropped them.
        if (!(entity instanceof LivingEntity) && !isLookable(entity)) {
            return false;
        }
        if (!entity.getWorld().equals(player.getWorld())) {
            return false;
        }
        if (entity.getLocation().distanceSquared(player.getLocation()) > range * range) {
            return false;
        }
        if (entity instanceof Player targetPlayer && !player.canSee(targetPlayer)) {
            return false;
        }
        if (!matchesTargetType(entity, targetType)) {
            return false;
        }
        return !requireLineOfSight || player.hasLineOfSight(entity);
    }

    private boolean matchesTargetType(Entity entity, LookTargetType targetType) {
        return switch (targetType) {
            case ANY -> true;
            case PLAYER -> entity instanceof Player;
            case MONSTER -> entity instanceof Monster;
        };
    }

    private void lockTarget(Player player, Entity target, LookTargetType targetType, String targetName) {
        lockedTargets.put(player.getUniqueId(), new LockedTarget(target.getUniqueId(), targetType, targetName, -1));
    }

    private LookTargetType targetTypeFor(Entity entity) {
        if (entity instanceof Player) {
            return LookTargetType.PLAYER;
        }
        if (entity instanceof Monster) {
            return LookTargetType.MONSTER;
        }
        return LookTargetType.ANY;
    }

    private void lookAtEntity(Player player, Entity target, boolean smooth) {
        if (!smooth) {
            player.lookAt(target, LookAnchor.EYES, LookAnchor.EYES);
            return;
        }
        rotateToward(player, entityLookLocation(target), LOOK_ENTITY_MAX_YAW_STEP, LOOK_ENTITY_MAX_PITCH_STEP);
    }

    private Location entityLookLocation(Entity entity) {
        if (entity instanceof LivingEntity livingEntity) {
            return livingEntity.getEyeLocation();
        }
        return entity.getLocation();
    }

    private void rotateToward(Player player, Location target, float maxYawStep, float maxPitchStep) {
        Location source = player.getEyeLocation();
        Vector difference = target.toVector().subtract(source.toVector());
        if (difference.lengthSquared() == 0.0D) {
            return;
        }

        double horizontal = Math.sqrt(difference.getX() * difference.getX() + difference.getZ() * difference.getZ());
        float targetYaw = normalizeYaw((float) Math.toDegrees(Math.atan2(-difference.getX(), difference.getZ())));
        float targetPitch = clampPitch((float) Math.toDegrees(-Math.atan2(difference.getY(), horizontal)));
        Location current = player.getLocation();
        player.setRotation(
                approachAngle(current.getYaw(), targetYaw, maxYawStep),
                approach(current.getPitch(), targetPitch, maxPitchStep)
        );
    }

    private float approach(float current, float target, float maxStep) {
        float delta = target - current;
        if (Math.abs(delta) <= maxStep) {
            return target;
        }
        return current + (float) Math.copySign(maxStep, delta);
    }

    private float approachAngle(float current, float target, float maxStep) {
        float delta = wrapDegrees(target - current);
        if (Math.abs(delta) <= maxStep) {
            return target;
        }
        return normalizeYaw(current + (float) Math.copySign(maxStep, delta));
    }

    private float parseRotationCoordinate(String raw, float base) {
        try {
            if (raw.equals("~")) {
                return base;
            }
            if (raw.startsWith("~")) {
                return base + Float.parseFloat(raw.substring(1));
            }
            return Float.parseFloat(raw);
        } catch (NumberFormatException ex) {
            throw new LocalizedException("error.invalid-number", raw);
        }
    }

    private float normalizeYaw(float yaw) {
        return wrapDegrees(yaw);
    }

    private float wrapDegrees(float angle) {
        float wrapped = angle % 360.0F;
        if (wrapped >= 180.0F) {
            wrapped -= 360.0F;
        }
        if (wrapped < -180.0F) {
            wrapped += 360.0F;
        }
        return wrapped;
    }

    private float clampPitch(float pitch) {
        return Math.max(-90.0F, Math.min(90.0F, pitch));
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
        for (String action : java.util.List.of("attack", "jump", "look", "move", "mine", "sneak", "mount", "use")) {
            resetAction(dummy, action);
        }
    }

    private boolean resetAction(DummyInstance dummy, String action) {
        Player player = dummy.player();
        switch (action) {
            case "attack", "look" -> lockedTargets.remove(player.getUniqueId());
            case "jump" -> player.setJumping(false);
            case "move" -> player.setVelocity(player.getVelocity().setX(0.0D).setZ(0.0D));
            case "mine" -> mineStates.remove(player.getUniqueId());
            case "sneak" -> player.setSneaking(false);
            case "mount" -> player.leaveVehicle();
            case "use" -> player.clearActiveItem();
            default -> {
                return false;
            }
        }
        dummyManager.markDirty(dummy.name());
        return true;
    }

    private void stopTask(UUID uuid, String action, DummyInstance dummy) {
        Map<String, ActiveAction> dummyTasks = tasks.get(uuid);
        ActiveAction active = dummyTasks == null ? null : dummyTasks.remove(action);
        if (active != null) {
            active.cancel();
        }
        if (dummyTasks != null && dummyTasks.isEmpty()) {
            tasks.remove(uuid);
        }
        if (dummy != null) {
            resetAction(dummy, action);
        } else {
            if (action.equals("mine")) {
                mineStates.remove(uuid);
            }
            if (action.equals("attack") || action.equals("look")) {
                lockedTargets.remove(uuid);
            }
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

    private record LockedTarget(UUID targetUuid, LookTargetType targetType, String targetName, int obscuredSinceTick) {
        private boolean matches(LookTargetType requestedType, String requestedName) {
            if (requestedName != null) {
                return targetType == LookTargetType.PLAYER
                        && targetName != null
                        && targetName.equalsIgnoreCase(requestedName);
            }
            return requestedType == LookTargetType.ANY || targetType == requestedType;
        }

        private LockedTarget withObscuredSinceTick(int tick) {
            return new LockedTarget(targetUuid, targetType, targetName, tick);
        }
    }

    private record TargetResolution(Entity target, boolean waitingForLockedTarget) {
        private static TargetResolution none() {
            return new TargetResolution(null, false);
        }

        private static TargetResolution waiting() {
            return new TargetResolution(null, true);
        }
    }

    private enum LookTargetType {
        ANY,
        PLAYER,
        MONSTER
    }

    /**
     * Holds a scheduled repeat action alongside the raw input needed to
     * recreate it, so it can be persisted across plugin restarts and
     * replayed via {@link #restoreActions}.
     */
    private record ActiveAction(BukkitTask task, String action, String[] args, int intervalTicks, int durationTicks) {
        void cancel() {
            task.cancel();
        }

        /**
         * Reconstruct the {@code /dummy <name> actions ...} command body
         * that originally scheduled this action.
         */
        String toCommand() {
            StringBuilder sb = new StringBuilder(action);
            for (String arg : args) {
                sb.append(' ').append(arg);
            }
            sb.append(" repeat");
            if (intervalTicks > 0 && intervalTicks != DummyActionService.defaultRepeatInterval(action)) {
                sb.append(" interval:").append(intervalTicks);
            }
            if (durationTicks > 0) {
                sb.append(" duration:").append(durationTicks);
            }
            return sb.toString();
        }
    }
}
