package io.github.railgun19457.dummy.persistence.sqlite;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class SqliteDatabase implements AutoCloseable {

    private final String jdbcUrl;

    public SqliteDatabase(JavaPlugin plugin) {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            throw new IllegalStateException("Unable to create plugin data directory");
        }
        File dbFile = new File(dataFolder, "dummy.db");
        this.jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
    }

    public void initialize() {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS dummy_profile (
                        id TEXT PRIMARY KEY,
                        name TEXT NOT NULL,
                        owner_id TEXT NOT NULL,
                        owner_name TEXT NOT NULL,
                        world_name TEXT NOT NULL,
                        x REAL NOT NULL,
                        y REAL NOT NULL,
                        z REAL NOT NULL,
                        yaw REAL NOT NULL,
                        pitch REAL NOT NULL,
                        skin_source_player_name TEXT,
                        skin_texture TEXT,
                        skin_signature TEXT,
                        trait_collidable INTEGER NOT NULL,
                        trait_invulnerable INTEGER NOT NULL,
                        trait_auto_restock INTEGER NOT NULL,
                        trait_allow_inventory_open INTEGER NOT NULL,
                        trait_allow_interaction_configure INTEGER NOT NULL,
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS dummy_inventory (
                        dummy_id TEXT PRIMARY KEY,
                        contents TEXT NOT NULL,
                        armor_contents TEXT NOT NULL,
                        offhand_item TEXT,
                        FOREIGN KEY(dummy_id) REFERENCES dummy_profile(id) ON DELETE CASCADE
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS dummy_runtime_snapshot (
                        dummy_id TEXT PRIMARY KEY,
                        spawned INTEGER NOT NULL,
                        entity_id INTEGER,
                        current_action TEXT,
                        sleeping INTEGER NOT NULL,
                        sneaking INTEGER NOT NULL,
                        sprinting INTEGER NOT NULL,
                        mounted_target INTEGER,
                        FOREIGN KEY(dummy_id) REFERENCES dummy_profile(id) ON DELETE CASCADE
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS dummy_experience (
                        dummy_id TEXT PRIMARY KEY,
                        stored_exp INTEGER NOT NULL,
                        FOREIGN KEY(dummy_id) REFERENCES dummy_profile(id) ON DELETE CASCADE
                    )
                    """);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to initialize SQLite schema", exception);
        }
    }

    public Connection openConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    @Override
    public void close() {
    }
}
