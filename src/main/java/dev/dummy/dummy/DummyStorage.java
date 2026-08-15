package dev.dummy.dummy;

import dev.dummy.DummyPlugin;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

/**
 * SQLite 持久化存储实现。
 *
 * <p>把原来基于 {@code dummies.yml} 全量重写的存储层换成 SQLite，每次保存只更新对应行。
 * WAL 模式允许读写互不阻塞，构造时若检测到旧 yml 文件会自动迁移。</p>
 */
public final class DummyStorage {
    private static final String SCHEMA_SQL = """
            CREATE TABLE IF NOT EXISTS dummies (
                name             TEXT PRIMARY KEY COLLATE NOCASE,
                uuid             TEXT NOT NULL,
                creator_uuid     TEXT,
                creator_name     TEXT NOT NULL,
                display_name     TEXT NOT NULL,
                world            TEXT NOT NULL,
                x                REAL NOT NULL,
                y                REAL NOT NULL,
                z                REAL NOT NULL,
                yaw              REAL NOT NULL,
                pitch            REAL NOT NULL,
                invulnerable     INTEGER NOT NULL,
                collision        INTEGER NOT NULL,
                ghost            INTEGER NOT NULL,
                chunk_loader     INTEGER NOT NULL,
                show_in_tab      INTEGER NOT NULL,
                name_format      TEXT NOT NULL,
                skin_type        TEXT NOT NULL,
                skin_value       TEXT NOT NULL,
                skin_signature   TEXT NOT NULL,
                skin_model_parts INTEGER NOT NULL,
                skin_fetched_at  INTEGER NOT NULL,
                storage_contents BLOB,
                armor_contents   BLOB,
                offhand_item     BLOB,
                exp_level        INTEGER NOT NULL,
                exp_progress     REAL NOT NULL,
                exp_total        INTEGER NOT NULL,
                state            TEXT NOT NULL
            )
            """;
    private static final String INDEX_STATE_SQL = "CREATE INDEX IF NOT EXISTS idx_dummies_state ON dummies(state)";
    private static final String INDEX_UUID_SQL = "CREATE INDEX IF NOT EXISTS idx_dummies_uuid ON dummies(uuid)";
    private static final String UPSERT_SQL = """
            INSERT OR REPLACE INTO dummies (
                name, uuid, creator_uuid, creator_name, display_name,
                world, x, y, z, yaw, pitch,
                invulnerable, collision, ghost, chunk_loader, show_in_tab, name_format,
                skin_type, skin_value, skin_signature, skin_model_parts, skin_fetched_at,
                storage_contents, armor_contents, offhand_item,
                exp_level, exp_progress, exp_total, state
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String SELECT_ACTIVE_SQL = "SELECT * FROM dummies WHERE state = 'active'";
    private static final String SELECT_REMOVED_SQL = "SELECT * FROM dummies WHERE name = ? COLLATE NOCASE AND state = 'removed'";
    private static final String DELETE_BY_NAME_SQL = "DELETE FROM dummies WHERE name = ? COLLATE NOCASE";

    private final DummyPlugin plugin;
    private final File file;
    private final File legacyFile;
    private final Connection connection;
    private final ReentrantLock writeLock = new ReentrantLock();

    public DummyStorage(DummyPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "dummies.db");
        this.legacyFile = new File(plugin.getDataFolder(), "dummies.yml");
        try {
            this.connection = openConnection(file);
            ensureSchema();
            if (plugin.getConfig().getBoolean("storage.migrate-from-yml", true) && legacyFile.isFile() && isEmpty()) {
                migrateFromYaml();
            }
        } catch (SQLException | IOException ex) {
            throw new RuntimeException("Failed to initialize Dummy SQLite storage", ex);
        }
    }

    private Connection openConnection(File dbFile) throws SQLException {
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        Properties props = new Properties();
        props.setProperty("journal_mode", "WAL");
        props.setProperty("synchronous", "NORMAL");
        props.setProperty("busy_timeout", "5000");
        Connection conn = DriverManager.getConnection(url, props);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode = WAL");
            stmt.execute("PRAGMA synchronous = NORMAL");
            stmt.execute("PRAGMA busy_timeout = 5000");
            stmt.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }

    private void ensureSchema() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(SCHEMA_SQL);
            stmt.execute(INDEX_STATE_SQL);
            stmt.execute(INDEX_UUID_SQL);
        }
    }

    public List<DummyRecord> load() {
        List<DummyRecord> records = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(SELECT_ACTIVE_SQL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                DummyRecord record = readRecord(rs);
                if (record != null) {
                    records.add(record);
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load active dummies", ex);
        }
        return records;
    }

    public DummyRecord loadRemoved(String name) {
        try (PreparedStatement ps = connection.prepareStatement(SELECT_REMOVED_SQL)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return readRecord(rs);
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load removed dummy " + name, ex);
        }
        return null;
    }

    /**
     * 同步 upsert 单条记录。
     * 必须在主线程调用（DummyInstance.player().getInventory() 是 Bukkit API）。
     */
    public void upsert(DummyInstance dummy) {
        Snapshot snapshot = snapshot(dummy, "active");
        upsertRecord(snapshot.record, snapshot.state);
    }

    /**
     * 标记假人为 removed（保留数据）。
     */
    public void markRemoved(DummyInstance dummy) {
        Snapshot snapshot = snapshot(dummy, "removed");
        upsertRecord(snapshot.record, snapshot.state);
    }

    /**
     * 兼容旧 API：把 RemovedRecord 一起存到 db。
     */
    public void saveRemoved(DummyInstance dummy) {
        markRemoved(dummy);
    }

    public void deleteRemoved(String name) {
        writeLock.lock();
        try (PreparedStatement ps = connection.prepareStatement(DELETE_BY_NAME_SQL)) {
            ps.setString(1, name);
            ps.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete dummy record " + name, ex);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 同步 upsert 单条带状态的记录。供 DummyManager.flushNow 使用。
     */
    public void upsertRecord(DummyRecord record, String state) {
        writeLock.lock();
        try (PreparedStatement ps = connection.prepareStatement(UPSERT_SQL)) {
            bindRecord(ps, record, state);
            ps.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to upsert dummy " + record.name(), ex);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 批量 upsert 多条记录到单事务。供 DummyManager.flushNow 使用。
     */
    public void upsertBatch(List<Snapshot> snapshots) {
        if (snapshots.isEmpty()) {
            return;
        }
        writeLock.lock();
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement ps = connection.prepareStatement(UPSERT_SQL)) {
                for (Snapshot entry : snapshots) {
                    bindRecord(ps, entry.record, entry.state);
                    ps.addBatch();
                }
                ps.executeBatch();
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to upsert batch of " + snapshots.size() + " dummies", ex);
        } finally {
            writeLock.unlock();
        }
    }

    private void bindRecord(PreparedStatement ps, DummyRecord record, String state) throws SQLException {
        int i = 1;
        ps.setString(i++, record.name());
        ps.setString(i++, record.uuid().toString());
        ps.setString(i++, record.creatorUuid() == null ? null : record.creatorUuid().toString());
        ps.setString(i++, record.creatorName());
        ps.setString(i++, record.name());
        ps.setString(i++, record.location().getWorld().getName());
        ps.setDouble(i++, record.location().getX());
        ps.setDouble(i++, record.location().getY());
        ps.setDouble(i++, record.location().getZ());
        ps.setDouble(i++, record.location().getYaw());
        ps.setDouble(i++, record.location().getPitch());
        DummySettings settings = record.settings();
        ps.setInt(i++, settings.invulnerable() ? 1 : 0);
        ps.setInt(i++, settings.collision() ? 1 : 0);
        ps.setInt(i++, settings.ghost() ? 1 : 0);
        ps.setInt(i++, settings.chunkLoader() ? 1 : 0);
        ps.setInt(i++, settings.showInTab() ? 1 : 0);
        ps.setString(i++, settings.nameFormat());
        DummySkin skin = record.skin();
        ps.setString(i++, skin.type());
        ps.setString(i++, skin.value());
        ps.setString(i++, skin.signature());
        ps.setInt(i++, skin.modelParts());
        ps.setLong(i++, skin.fetchedAt());
        ps.setBytes(i++, serializeItems(record.storageContents()));
        ps.setBytes(i++, serializeItems(record.armorContents()));
        ps.setBytes(i++, serializeItem(record.offhandItem()));
        DummyExperience experience = record.experience();
        ps.setInt(i++, experience.level());
        ps.setFloat(i++, experience.progress());
        ps.setInt(i++, experience.total());
        ps.setString(i, state);
    }

    private DummyRecord readRecord(ResultSet rs) throws SQLException {
        String nameKey = rs.getString("name");
        String worldName = rs.getString("world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("Skipping dummy '" + nameKey + "': world '" + worldName + "' is not loaded");
            return null;
        }
        UUID uuid = parseUuid(rs.getString("uuid"), nameKey);
        UUID creatorUuid = parseNullableUuid(rs.getString("creator_uuid"));
        String creatorName = rs.getString("creator_name");
        String displayName = rs.getString("display_name");
        Location location = new Location(
                world,
                rs.getDouble("x"),
                rs.getDouble("y"),
                rs.getDouble("z"),
                (float) rs.getDouble("yaw"),
                (float) rs.getDouble("pitch")
        );
        DummySettings settings = new DummySettings(
                rs.getBoolean("invulnerable"),
                rs.getBoolean("collision"),
                rs.getBoolean("ghost"),
                rs.getBoolean("chunk_loader"),
                rs.getBoolean("show_in_tab"),
                rs.getString("name_format")
        );
        DummySkin skin = new DummySkin(
                rs.getString("skin_type"),
                rs.getString("skin_value"),
                rs.getString("skin_signature"),
                rs.getInt("skin_model_parts"),
                rs.getLong("skin_fetched_at")
        );
        ItemStack[] storageContents = deserializeItems(rs.getBytes("storage_contents"));
        ItemStack[] armorContents = deserializeItems(rs.getBytes("armor_contents"));
        ItemStack offhandItem = deserializeItem(rs.getBytes("offhand_item"));
        DummyExperience experience = new DummyExperience(
                rs.getInt("exp_level"),
                rs.getFloat("exp_progress"),
                rs.getInt("exp_total")
        );
        return new DummyRecord(uuid, creatorUuid, creatorName, displayName, location, settings, skin, storageContents, armorContents, offhandItem, experience);
    }

    /**
     * 在主线程采集 DummyInstance 的快照，用于异步 upsert。
     * 必须在主线程调用：会读 PlayerInventory。
     */
    public Snapshot snapshot(DummyInstance dummy, String state) {
        return new Snapshot(snapshotRecord(dummy), state);
    }

    private DummyRecord snapshotRecord(DummyInstance dummy) {
        Location location = dummy.location();
        org.bukkit.inventory.PlayerInventory inventory = dummy.player().getInventory();
        return new DummyRecord(
                dummy.uuid(),
                dummy.creatorUuid(),
                dummy.creatorName(),
                dummy.name(),
                location.clone(),
                dummy.settings(),
                dummy.skin(),
                copyArray(inventory.getStorageContents()),
                copyArray(inventory.getArmorContents()),
                inventory.getItemInOffHand() == null ? null : inventory.getItemInOffHand().clone(),
                DummyExperience.fromPlayer(dummy.player())
        );
    }

    private ItemStack[] copyArray(ItemStack[] source) {
        if (source == null) {
            return null;
        }
        ItemStack[] copy = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i] == null ? null : source[i].clone();
        }
        return copy;
    }

    private byte[] serializeItem(ItemStack item) {
        if (item == null) {
            return null;
        }
        return serializeItems(new ItemStack[]{item});
    }

    private byte[] serializeItems(ItemStack[] items) {
        if (items == null) {
            return null;
        }
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             BukkitObjectOutputStream out = new BukkitObjectOutputStream(bytes)) {
            out.writeObject(items);
            out.flush();
            return bytes.toByteArray();
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to serialize ItemStack[]", ex);
            return null;
        }
    }

    private ItemStack deserializeItem(byte[] blob) {
        ItemStack[] items = deserializeItems(blob);
        return items == null || items.length == 0 ? null : items[0];
    }

    private ItemStack[] deserializeItems(byte[] blob) {
        if (blob == null) {
            return null;
        }
        try (ByteArrayInputStream bytes = new ByteArrayInputStream(blob);
             BukkitObjectInputStream in = new BukkitObjectInputStream(bytes)) {
            Object obj = in.readObject();
            if (obj instanceof ItemStack[] array) {
                return array;
            }
            if (obj instanceof ItemStack single) {
                return new ItemStack[]{single};
            }
            plugin.getLogger().warning("Unexpected item type in blob: " + (obj == null ? "null" : obj.getClass().getName()));
            return null;
        } catch (IOException | ClassNotFoundException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to deserialize ItemStack[]", ex);
            return null;
        }
    }

    private UUID parseUuid(String value, String key) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Skipping dummy '" + key + "': invalid uuid '" + value + "'");
            return null;
        }
    }

    private UUID parseNullableUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean isEmpty() throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM dummies")) {
            return rs.next() && rs.getInt(1) == 0;
        }
    }

    /**
     * 从旧 dummies.yml 迁移到 SQLite。完成后会把 yml 重命名为 .bak 避免重复迁移。
     */
    private void migrateFromYaml() throws SQLException, IOException {
        if (!legacyFile.isFile()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(legacyFile);
        List<Snapshot> batch = new ArrayList<>();
        collectForMigration(batch, config.getConfigurationSection("dummies"), "active");
        collectForMigration(batch, config.getConfigurationSection("removed"), "removed");
        if (batch.isEmpty()) {
            plugin.getLogger().info("No dummies to migrate from legacy dummies.yml");
            renameLegacy();
            return;
        }
        upsertBatch(batch);
        plugin.getLogger().info("Migrated " + batch.size() + " dummies from dummies.yml to dummies.db");
        renameLegacy();
    }

    private void collectForMigration(List<Snapshot> batch, ConfigurationSection section, String state) {
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection child = section.getConfigurationSection(key);
            if (child == null) {
                continue;
            }
            DummyRecord record = readLegacyRecord(child, key);
            if (record != null) {
                batch.add(new Snapshot(record, state));
            }
        }
    }

    private DummyRecord readLegacyRecord(ConfigurationSection section, String key) {
        String worldName = section.getString("world", "");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("Skipping dummy '" + key + "': world '" + worldName + "' is not loaded during migration");
            return null;
        }
        UUID uuid = parseUuid(section.getString("uuid"), key);
        UUID creatorUuid = parseNullableUuid(section.getString("creator.uuid"));
        String creatorName = section.getString("creator.name", "console");
        String name = section.getString("name", key);
        Location location = new Location(
                world,
                section.getDouble("x"),
                section.getDouble("y"),
                section.getDouble("z"),
                (float) section.getDouble("yaw"),
                (float) section.getDouble("pitch")
        );
        ConfigurationSection settingsSection = section.getConfigurationSection("settings");
        DummySettings settings = settingsSection == null
                ? DummySettings.defaults(plugin.getConfig())
                : DummySettings.fromConfig(settingsSection);
        DummySkin skin = DummySkin.fromConfig(section.getConfigurationSection("skin"));
        DummyExperience experience = DummyExperience.fromConfig(section.getConfigurationSection("experience"));
        ItemStack[] storageContents = readLegacyItems(section, "inventory.storage");
        ItemStack[] armorContents = readLegacyItems(section, "inventory.armor");
        ItemStack offhandItem = section.getItemStack("inventory.offhand");
        return new DummyRecord(uuid, creatorUuid, creatorName, name, location, settings, skin, storageContents, armorContents, offhandItem, experience);
    }

    private ItemStack[] readLegacyItems(ConfigurationSection section, String path) {
        List<?> list = section.getList(path, List.of());
        ItemStack[] items = new ItemStack[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            if (item instanceof ItemStack itemStack) {
                items[i] = itemStack;
            }
        }
        return items;
    }

    private void renameLegacy() throws IOException {
        File backup = new File(plugin.getDataFolder(), "dummies.yml.bak");
        if (backup.exists() && !backup.delete()) {
            plugin.getLogger().warning("Failed to delete old backup " + backup.getName() + " before migration backup");
        }
        if (legacyFile.isFile() && legacyFile.renameTo(backup)) {
            plugin.getLogger().info("Renamed legacy dummies.yml to dummies.yml.bak");
        }
    }

    public void close() {
        writeLock.lock();
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to close Dummy storage connection", ex);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 在不可信任保存上下文中包装一个 snapshot。供 DummyManager 异步 flush 使用。
     */
    public static final class Snapshot {
        public final DummyRecord record;
        public final String state;

        public Snapshot(DummyRecord record, String state) {
            this.record = record;
            this.state = state;
        }
    }
}

