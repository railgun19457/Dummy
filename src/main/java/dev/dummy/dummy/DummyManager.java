package dev.dummy.dummy;

import dev.dummy.DummyPlugin;
import dev.dummy.i18n.LocalizedException;
import dev.dummy.nms.DummyHandle;
import dev.dummy.nms.FakePlayerAdapter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitTask;

public final class DummyManager {
    public static final String PROXY_TAB_CHANNEL = "proxytab:virtual_players";

    private static final Pattern NAME_PATTERN = Pattern.compile("[A-Za-z0-9_]{3,16}");
    private static final int PROXY_TAB_PROTOCOL_VERSION = 1;

    private final DummyPlugin plugin;
    private final FakePlayerAdapter adapter;
    private final DummyStorage storage;
    private final Map<String, DummyInstance> dummiesByName = new LinkedHashMap<>();
    private final Map<UUID, DummyInstance> dummiesByUuid = new LinkedHashMap<>();
    private final Map<UUID, LoadedChunkTickets> chunkTickets = new LinkedHashMap<>();
    private final Map<ChunkTicket, Integer> chunkTicketRefs = new LinkedHashMap<>();
    private final BukkitTask chunkTicketRefreshTask;
    private boolean shuttingDown;

    public DummyManager(DummyPlugin plugin, FakePlayerAdapter adapter, DummyStorage storage) {
        this.plugin = plugin;
        this.adapter = adapter;
        this.storage = storage;
        this.chunkTicketRefreshTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::refreshChunkTickets, 20L, 20L);
    }

    public DummyInstance spawn(CommandSender sender, String name, Location location) {
        return spawn(sender, name, location, DummySkin.NONE);
    }

    public DummyInstance spawn(CommandSender sender, String name, Location location, DummySkin skin) {
        validateName(name);
        enforceSpawnLimits(sender);
        DummyRecord removed = plugin.getConfig().getBoolean("storage.keep-removed-data", true) ? storage.loadRemoved(name) : null;
        if (removed != null) {
            DummyInstance dummy = spawn(
                    sender,
                    removed.uuid(),
                    creatorUuid(sender),
                    creatorName(sender),
                    name,
                    location,
                    removed.settings(),
                    removed.skin(),
                    true,
                    false
            );
            dummy.applyRecord(removed);
            storage.deleteRemoved(name);
            save();
            return dummy;
        }
        return spawn(sender, uuidForName(name), creatorUuid(sender), creatorName(sender), name, location, DummySettings.defaults(plugin.getConfig()), skin, true, true);
    }

    public void restoreSavedDummies() {
        for (DummyRecord record : storage.load()) {
            if (contains(record.name())) {
                continue;
            }
            try {
                DummyInstance dummy = spawn(Bukkit.getConsoleSender(), record.uuid(), record.creatorUuid(), record.creatorName(), record.name(), record.location(), record.settings(), record.skin(), false, false);
                dummy.applyRecord(record);
            } catch (RuntimeException ex) {
                plugin.getLogger().warning("Failed to restore dummy '" + record.name() + "': " + ex.getMessage());
            }
        }
        save();
    }

    public boolean remove(String name, String reason) {
        DummyInstance dummy = dummiesByName.remove(normalize(name));
        if (dummy == null) {
            return false;
        }
        releaseChunkTicket(dummy);
        dropInventoryIfConfigured(dummy);
        storage.saveRemoved(dummy);
        dummiesByUuid.remove(dummy.uuid());
        sendProxyTabRemove(dummy);
        dummy.handle().remove(Component.text("[Dummy] " + reason));
        broadcastQuit(dummy);
        save();
        return true;
    }

    public int removeAll(String reason) {
        List<DummyInstance> snapshot = new ArrayList<>(dummiesByName.values());
        for (DummyInstance dummy : snapshot) {
            releaseChunkTicket(dummy);
            dropInventoryIfConfigured(dummy);
            storage.saveRemoved(dummy);
            sendProxyTabRemove(dummy);
            dummy.handle().remove(Component.text("[Dummy] " + reason));
            broadcastQuit(dummy);
        }
        dummiesByName.clear();
        dummiesByUuid.clear();
        save();
        return snapshot.size();
    }

    public void shutdown(String reason) {
        shuttingDown = true;
        chunkTicketRefreshTask.cancel();
        List<DummyInstance> snapshot = new ArrayList<>(dummiesByName.values());
        for (DummyInstance dummy : snapshot) {
            dropInventoryIfConfigured(dummy);
        }
        save();
        for (DummyInstance dummy : snapshot) {
            releaseChunkTicket(dummy);
            sendProxyTabRemove(dummy);
            dummy.handle().remove(Component.text("[Dummy] " + reason));
        }
        dummiesByName.clear();
        dummiesByUuid.clear();
        shuttingDown = false;
    }

    public void cleanup(Player player) {
        DummyInstance dummy = dummiesByUuid.remove(player.getUniqueId());
        if (dummy == null) {
            return;
        }
        releaseChunkTicket(dummy);
        dropInventoryIfConfigured(dummy);
        sendProxyTabRemove(dummy);
        dummiesByName.remove(normalize(dummy.name()));
        if (!shuttingDown) {
            save();
        }
    }

    public Collection<DummyInstance> all() {
        return dummiesByName.values()
                .stream()
                .sorted(Comparator.comparing(DummyInstance::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<String> names() {
        return all().stream().map(DummyInstance::name).toList();
    }

    public List<String> activeNames() {
        return all().stream()
                .filter(this::isActive)
                .map(DummyInstance::name)
                .toList();
    }

    public DummyInstance get(String name) {
        return dummiesByName.get(normalize(name));
    }

    public DummyInstance get(UUID uuid) {
        return dummiesByUuid.get(uuid);
    }

    public void handleDeath(Player player) {
        DummyInstance dummy = get(player.getUniqueId());
        if (dummy == null) {
            return;
        }
        boolean autoResummon = plugin.getConfig().getBoolean("death.auto-resummon", false);
        DummyRecord record = autoResummon ? snapshot(dummy, resummonLocation(player)) : null;
        removeDeadDummy(dummy, !autoResummon);
        save();

        if (record != null) {
            Bukkit.getScheduler().runTask(plugin, () -> restoreResummoned(record));
        }
    }

    public boolean contains(String name) {
        return dummiesByName.containsKey(normalize(name));
    }

    public boolean isDummy(Player player) {
        return dummiesByUuid.containsKey(player.getUniqueId());
    }

    public void applyTabVisibility(Player viewer) {
        for (DummyInstance dummy : dummiesByName.values()) {
            applyTabVisibility(viewer, dummy);
        }
    }

    public void syncProxyTab() {
        sendProxyTabReset();
        for (DummyInstance dummy : dummiesByName.values()) {
            sendProxyTabUpdate(dummy);
        }
    }

    public void save() {
        storage.save(dummiesByName.values());
    }

    public DummySettings updateSettings(String name, String key, String value) {
        DummyInstance dummy = require(name);
        DummySettings settings = dummy.settings().with(key, value);
        dummy.settings(settings);
        dummy.handle().applySettings(dummy.name(), settings);
        updateChunkTicket(dummy);
        refreshTabVisibility(dummy);
        sendProxyTabUpdate(dummy);
        save();
        return settings;
    }

    public void setSkin(String name, DummySkin skin) {
        DummyInstance dummy = require(name);
        dummy.skin(skin);
        dummy.handle().applySkin(skin);
        refreshTabVisibility(dummy);
        sendProxyTabUpdate(dummy);
        save();
    }

    public void teleportDummy(String name, Location location) {
        DummyInstance dummy = require(name);
        dummy.handle().teleport(location);
        updateChunkTicket(dummy);
        save();
    }

    public void teleportPlayerToDummy(Player player, String name) {
        DummyInstance dummy = require(name);
        player.teleport(dummy.location());
    }

    public void swap(Player player, String name) {
        DummyInstance dummy = require(name);
        Location playerLocation = player.getLocation();
        Location dummyLocation = dummy.location();
        player.teleport(dummyLocation);
        dummy.handle().teleport(playerLocation);
        updateChunkTicket(dummy);
        save();
    }

    public int transferExperience(String name, Player receiver, boolean all, int amount) {
        DummyInstance dummy = require(name);
        Player source = dummy.player();
        int available = Math.max(0, source.getTotalExperience());
        int transferred = all ? available : Math.min(Math.max(0, amount), available);
        if (transferred <= 0) {
            return 0;
        }
        source.setLevel(0);
        source.setExp(0.0F);
        source.setTotalExperience(0);
        source.giveExp(available - transferred, false);
        receiver.giveExp(transferred, true);
        save();
        return transferred;
    }

    public DummyInstance require(String name) {
        DummyInstance dummy = get(name);
        if (dummy == null) {
            throw new LocalizedException("error.dummy-not-found", name);
        }
        return dummy;
    }

    private DummyInstance spawn(
            CommandSender sender,
            UUID uuid,
            UUID creatorUuid,
            String creatorName,
            String name,
            Location location,
            DummySettings settings,
            DummySkin skin,
            boolean runCommands,
            boolean save
    ) {
        validateName(name);
        if (contains(name)) {
            throw new LocalizedException("error.dummy-exists", name);
        }
        if (Bukkit.getPlayerExact(name) != null) {
            throw new LocalizedException("error.real-player-online", name);
        }
        if (location.getWorld() == null) {
            throw new LocalizedException("error.spawn-no-world");
        }
        if (runCommands) {
            runConfiguredCommands("commands.before-spawn", uuid, creatorUuid, creatorName, name, location);
        }
        DummyCreateRequest request = new DummyCreateRequest(uuid, name, location.clone(), settings, skin);
        DummyHandle handle = adapter.spawn(request);
        DummyInstance dummy = new DummyInstance(uuid, creatorUuid, creatorName, name, settings, skin, handle);
        dummiesByName.put(normalize(name), dummy);
        dummiesByUuid.put(uuid, dummy);
        updateChunkTicket(dummy);
        refreshTabVisibility(dummy);
        sendProxyTabUpdate(dummy);
        if (runCommands) {
            runConfiguredCommands("commands.after-spawn", uuid, creatorUuid, creatorName, name, location);
        }
        if (sender != null) {
            plugin.getLogger().info(sender.getName() + " spawned dummy " + name + " at " + formatLocation(location));
        }
        if (save) {
            save();
        }
        return dummy;
    }

    private void refreshTabVisibility(DummyInstance dummy) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            applyTabVisibility(viewer, dummy);
        }
    }

    private void applyTabVisibility(Player viewer, DummyInstance dummy) {
        if (viewer.getUniqueId().equals(dummy.uuid()) || isDummy(viewer)) {
            return;
        }
        if (dummy.settings().showInTab()) {
            viewer.listPlayer(dummy.player());
            return;
        }
        viewer.listPlayer(dummy.player());
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            DummyInstance current = get(dummy.name());
            if (viewer.isOnline() && current != null && isActive(current) && !current.settings().showInTab()) {
                viewer.unlistPlayer(current.player());
            }
        }, 40L);
    }

    private void sendProxyTabUpdate(DummyInstance dummy) {
        if (!dummy.settings().showInTab()) {
            sendProxyTabHide(dummy);
            return;
        }
        sendProxyTabMessage(dummy.uuid(), output -> {
            output.writeUTF("update");
            output.writeLong(dummy.uuid().getMostSignificantBits());
            output.writeLong(dummy.uuid().getLeastSignificantBits());
            output.writeUTF(dummy.name());
            DummySkin skin = dummy.skin();
            output.writeUTF(skin.value());
            output.writeUTF(skin.signature());
            output.writeInt(Math.max(0, dummy.player().getPing()));
            output.writeInt(dummy.player().getGameMode().getValue());
        });
    }

    private void sendProxyTabHide(DummyInstance dummy) {
        sendProxyTabMessage(dummy.uuid(), output -> {
            output.writeUTF("hide");
            output.writeLong(dummy.uuid().getMostSignificantBits());
            output.writeLong(dummy.uuid().getLeastSignificantBits());
        });
    }

    private void sendProxyTabRemove(DummyInstance dummy) {
        sendProxyTabMessage(dummy.uuid(), output -> {
            output.writeUTF("remove");
            output.writeLong(dummy.uuid().getMostSignificantBits());
            output.writeLong(dummy.uuid().getLeastSignificantBits());
        });
    }

    private void sendProxyTabReset() {
        sendProxyTabMessage(null, output -> output.writeUTF("reset"));
    }

    private void sendProxyTabMessage(UUID excludedPlayer, ProxyTabPayload payload) {
        Player carrier = proxyTabCarrier(excludedPlayer);
        if (carrier == null) {
            return;
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeByte(PROXY_TAB_PROTOCOL_VERSION);
            payload.write(output);
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to encode ProxyTab virtual-player payload: " + ex.getMessage());
            return;
        }
        carrier.sendPluginMessage(plugin, PROXY_TAB_CHANNEL, bytes.toByteArray());
    }

    private Player proxyTabCarrier(UUID excludedPlayer) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if ((excludedPlayer == null || !player.getUniqueId().equals(excludedPlayer)) && !isDummy(player)) {
                return player;
            }
        }
        return null;
    }

    private void updateChunkTicket(DummyInstance dummy) {
        if (!dummy.settings().chunkLoader() || dummy.settings().ghost()) {
            releaseChunkTicket(dummy);
            return;
        }
        Location location = dummy.location();
        World world = location.getWorld();
        if (world == null) {
            releaseChunkTicket(dummy);
            return;
        }

        int centerX = location.getBlockX() >> 4;
        int centerZ = location.getBlockZ() >> 4;
        int radius = Math.max(0, world.getSimulationDistance());
        LoadedChunkTickets current = chunkTickets.get(dummy.uuid());
        if (current != null && current.matches(world, centerX, centerZ, radius)) {
            return;
        }

        releaseChunkTicket(dummy);
        Set<ChunkTicket> tickets = chunkTickets(world, centerX, centerZ, radius);
        for (ChunkTicket ticket : tickets) {
            addChunkTicket(ticket);
        }
        chunkTickets.put(dummy.uuid(), new LoadedChunkTickets(world, centerX, centerZ, radius, tickets));
    }

    private void releaseChunkTicket(DummyInstance dummy) {
        LoadedChunkTickets loaded = chunkTickets.remove(dummy.uuid());
        if (loaded != null) {
            for (ChunkTicket ticket : loaded.tickets()) {
                removeChunkTicket(ticket);
            }
        }
    }

    private void refreshChunkTickets() {
        for (DummyInstance dummy : new ArrayList<>(dummiesByName.values())) {
            updateChunkTicket(dummy);
        }
    }

    private Set<ChunkTicket> chunkTickets(World world, int centerX, int centerZ, int radius) {
        Set<ChunkTicket> tickets = new LinkedHashSet<>();
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                tickets.add(new ChunkTicket(world, x, z));
            }
        }
        return tickets;
    }

    private void addChunkTicket(ChunkTicket ticket) {
        int count = chunkTicketRefs.getOrDefault(ticket, 0);
        if (count == 0) {
            ticket.world().addPluginChunkTicket(ticket.x(), ticket.z(), plugin);
        }
        chunkTicketRefs.put(ticket, count + 1);
    }

    private void removeChunkTicket(ChunkTicket ticket) {
        int count = chunkTicketRefs.getOrDefault(ticket, 0);
        if (count <= 1) {
            chunkTicketRefs.remove(ticket);
            ticket.world().removePluginChunkTicket(ticket.x(), ticket.z(), plugin);
            return;
        }
        chunkTicketRefs.put(ticket, count - 1);
    }

    private void validateName(String name) {
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new LocalizedException("error.invalid-name");
        }
    }

    private void removeDeadDummy(DummyInstance dummy, boolean saveRemoved) {
        releaseChunkTicket(dummy);
        dummiesByName.remove(normalize(dummy.name()));
        dummiesByUuid.remove(dummy.uuid());
        if (saveRemoved) {
            storage.saveRemoved(dummy);
        }
        sendProxyTabRemove(dummy);
        dummy.handle().remove(Component.text("[Dummy] died"));
        broadcastQuit(dummy);
    }

    private boolean isActive(DummyInstance dummy) {
        Player player = dummy.player();
        return !player.isDead() && player.isValid();
    }

    private void restoreResummoned(DummyRecord record) {
        try {
            DummyInstance resummoned = spawn(
                    Bukkit.getConsoleSender(),
                    record.uuid(),
                    record.creatorUuid(),
                    record.creatorName(),
                    record.name(),
                    record.location(),
                    record.settings(),
                    record.skin(),
                    false,
                    false
            );
            resummoned.applyRecord(record);
            resummoned.handle().applySettings(resummoned.name(), resummoned.settings());
            updateChunkTicket(resummoned);
            save();
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Failed to resummon dummy '" + record.name() + "': " + ex.getMessage());
        }
    }

    private DummyRecord snapshot(DummyInstance dummy, Location location) {
        PlayerInventory inventory = dummy.player().getInventory();
        return new DummyRecord(
                dummy.uuid(),
                dummy.creatorUuid(),
                dummy.creatorName(),
                dummy.name(),
                location.clone(),
                dummy.settings(),
                dummy.skin(),
                copyItems(inventory.getStorageContents()),
                copyItems(inventory.getArmorContents()),
                copyItem(inventory.getItemInOffHand()),
                dummy.experience()
        );
    }

    private Location resummonLocation(Player player) {
        Location location = player.getRespawnLocation();
        if (location == null || location.getWorld() == null) {
            location = player.getWorld().getSpawnLocation();
        }
        return location.clone();
    }

    private ItemStack[] copyItems(ItemStack[] source) {
        ItemStack[] copy = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) {
            copy[i] = copyItem(source[i]);
        }
        return copy;
    }

    private ItemStack copyItem(ItemStack item) {
        return item == null || item.isEmpty() ? null : item.clone();
    }

    private void enforceSpawnLimits(CommandSender sender) {
        int serverLimit = plugin.getConfig().getInt("limits.server-total", -1);
        if (serverLimit >= 0 && dummiesByName.size() >= serverLimit) {
            throw new LocalizedException("error.server-limit-reached", serverLimit);
        }
        int playerLimit = plugin.getConfig().getInt("limits.per-player", -1);
        if (playerLimit < 0 || !(sender instanceof Player player)) {
            return;
        }
        long owned = dummiesByName.values()
                .stream()
                .filter(dummy -> player.getUniqueId().equals(dummy.creatorUuid()))
                .count();
        if (owned >= playerLimit) {
            throw new LocalizedException("error.player-limit-reached", playerLimit);
        }
    }

    private void dropInventoryIfConfigured(DummyInstance dummy) {
        if (!plugin.getConfig().getBoolean("inventory.drop-on-quit", false)) {
            return;
        }
        Player player = dummy.player();
        PlayerInventory inventory = player.getInventory();
        Location location = player.getLocation();
        for (ItemStack item : inventory.getStorageContents()) {
            dropItem(location, item);
        }
        for (ItemStack item : inventory.getArmorContents()) {
            dropItem(location, item);
        }
        dropItem(location, inventory.getItemInOffHand());
        inventory.setStorageContents(new ItemStack[inventory.getStorageContents().length]);
        inventory.setArmorContents(new ItemStack[inventory.getArmorContents().length]);
        inventory.setItemInOffHand(ItemStack.empty());
    }

    private void dropItem(Location location, ItemStack item) {
        if (item == null || item.isEmpty() || location.getWorld() == null) {
            return;
        }
        location.getWorld().dropItemNaturally(location, item.clone());
    }

    private void runConfiguredCommands(String path, UUID uuid, UUID creatorUuid, String creatorName, String name, Location location) {
        for (String command : plugin.getConfig().getStringList(path)) {
            String prepared = placeholders(command, uuid, creatorUuid, creatorName, name, location);
            if (prepared.startsWith("/")) {
                prepared = prepared.substring(1);
            }
            if (!prepared.isBlank()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), prepared);
            }
        }
    }

    private void broadcastQuit(DummyInstance dummy) {
        Bukkit.broadcast(Component.translatable("multiplayer.player.left", NamedTextColor.YELLOW, Component.text(dummy.name())));
    }

    private String placeholders(String command, UUID uuid, UUID creatorUuid, String creatorName, String name, Location location) {
        return command
                .replace("%dummy%", name)
                .replace("%uuid%", uuid.toString())
                .replace("%creator%", creatorName)
                .replace("%creator_uuid%", creatorUuid == null ? "" : creatorUuid.toString())
                .replace("%world%", location.getWorld() == null ? "" : location.getWorld().getName())
                .replace("%x%", Double.toString(location.getX()))
                .replace("%y%", Double.toString(location.getY()))
                .replace("%z%", Double.toString(location.getZ()));
    }

    private UUID creatorUuid(CommandSender sender) {
        return sender instanceof Player player ? player.getUniqueId() : null;
    }

    private String creatorName(CommandSender sender) {
        return sender == null ? "console" : sender.getName();
    }

    private String formatLocation(Location location) {
        return "%s %.1f %.1f %.1f".formatted(
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ()
        );
    }

    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public static UUID uuidForName(String name) {
        return UUID.nameUUIDFromBytes(("dummy:" + name.toLowerCase(Locale.ROOT)).getBytes(StandardCharsets.UTF_8));
    }

    @FunctionalInterface
    private interface ProxyTabPayload {
        void write(DataOutputStream output) throws IOException;
    }

    private record LoadedChunkTickets(World world, int centerX, int centerZ, int radius, Set<ChunkTicket> tickets) {
        private boolean matches(World world, int centerX, int centerZ, int radius) {
            return this.world.equals(world) && this.centerX == centerX && this.centerZ == centerZ && this.radius == radius;
        }
    }

    private record ChunkTicket(World world, int x, int z) {
    }
}
