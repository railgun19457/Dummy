package dev.dummy.dummy;

import dev.dummy.DummyPlugin;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class DummyStorage {
    private final DummyPlugin plugin;
    private final File file;

    public DummyStorage(DummyPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "dummies.yml");
    }

    public List<DummyRecord> load() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection("dummies");
        if (root == null) {
            return List.of();
        }

        List<DummyRecord> records = new ArrayList<>();
        for (String key : root.getKeys(false)) {
            DummyRecord record = readRecord(root.getConfigurationSection(key), key);
            if (record != null) {
                records.add(record);
            }
        }
        return records;
    }

    public DummyRecord loadRemoved(String name) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection("removed");
        if (root == null) {
            return null;
        }
        String key = name.toLowerCase(Locale.ROOT);
        return readRecord(root.getConfigurationSection(key), key);
    }

    public void save(Iterable<DummyInstance> dummies) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("dummies", null);
        ConfigurationSection root = config.createSection("dummies");
        for (DummyInstance dummy : dummies) {
            write(root.createSection(dummy.name().toLowerCase(Locale.ROOT)), dummy);
        }
        save(config);
    }

    public void saveRemoved(DummyInstance dummy) {
        if (!plugin.getConfig().getBoolean("storage.keep-removed-data", true)) {
            deleteRemoved(dummy.name());
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection("removed");
        if (root == null) {
            root = config.createSection("removed");
        }
        String key = dummy.name().toLowerCase(Locale.ROOT);
        root.set(key, null);
        write(root.createSection(key), dummy);
        save(config);
    }

    public void deleteRemoved(String name) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("removed." + name.toLowerCase(Locale.ROOT), null);
        save(config);
    }

    private DummyRecord readRecord(ConfigurationSection section, String key) {
        if (section == null) {
            return null;
        }

        String worldName = section.getString("world", "");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("Skipping dummy '" + key + "': world '" + worldName + "' is not loaded");
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
        return new DummyRecord(
                uuid,
                creatorUuid,
                creatorName,
                name,
                location,
                settings,
                skin,
                readItems(section, "inventory.storage"),
                readItems(section, "inventory.armor"),
                section.getItemStack("inventory.offhand"),
                experience
        );
    }

    private void write(ConfigurationSection section, DummyInstance dummy) {
        Location location = dummy.location();
        section.set("uuid", dummy.uuid().toString());
        section.set("creator.uuid", dummy.creatorUuid() == null ? null : dummy.creatorUuid().toString());
        section.set("creator.name", dummy.creatorName());
        section.set("name", dummy.name());
        section.set("world", location.getWorld().getName());
        section.set("x", location.getX());
        section.set("y", location.getY());
        section.set("z", location.getZ());
        section.set("yaw", location.getYaw());
        section.set("pitch", location.getPitch());

        DummySettings settings = dummy.settings();
        section.set("settings.invulnerable", settings.invulnerable());
        section.set("settings.collision", settings.collision());
        section.set("settings.ghost", settings.ghost());
        section.set("settings.chunk-loader", settings.chunkLoader());
        section.set("settings.show-in-tab", settings.showInTab());
        section.set("settings.name-format", settings.nameFormat());

        DummySkin skin = dummy.skin();
        section.set("skin.type", skin.type());
        section.set("skin.value", skin.value());
        section.set("skin.signature", skin.signature());
        section.set("skin.model-parts", skin.modelParts());
        section.set("skin.fetched-at", skin.fetchedAt());

        PlayerInventory inventory = dummy.player().getInventory();
        section.set("inventory.storage", new ArrayList<>(Arrays.asList(inventory.getStorageContents())));
        section.set("inventory.armor", new ArrayList<>(Arrays.asList(inventory.getArmorContents())));
        section.set("inventory.offhand", inventory.getItemInOffHand());

        DummyExperience experience = dummy.experience();
        section.set("experience.level", experience.level());
        section.set("experience.progress", experience.progress());
        section.set("experience.total", experience.total());
    }

    private ItemStack[] readItems(ConfigurationSection section, String path) {
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

    private void save(YamlConfiguration config) {
        try {
            config.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save dummies.yml", ex);
        }
    }

    private UUID parseUuid(String value, String fallbackName) {
        if (value != null) {
            try {
                return UUID.fromString(value);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return DummyManager.uuidForName(fallbackName);
    }

    private UUID parseNullableUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
