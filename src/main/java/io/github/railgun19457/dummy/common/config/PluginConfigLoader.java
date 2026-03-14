package io.github.railgun19457.dummy.common.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class PluginConfigLoader {

    private PluginConfigLoader() {
    }

    public static PluginConfig load(JavaPlugin plugin) {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        String defaultLocale = requireNonBlank(config.getString("plugin.default-locale"), "plugin.default-locale");
        int serverMaxDummies = requirePositive(config.getInt("limits.server-max-dummies"), "limits.server-max-dummies");
        int perPlayerMaxDummies = requirePositive(config.getInt("limits.per-player-max-dummies"), "limits.per-player-max-dummies");
        String nameTemplate = requireTemplate(config.getString("naming.default-name-template"), "naming.default-name-template");

        return new PluginConfig(
                defaultLocale,
                new LimitConfig(serverMaxDummies, perPlayerMaxDummies),
                new NamingConfig(nameTemplate),
                new SleepConfig(config.getBoolean("sleep.follow-owner-sleep")),
                new RemovalConfig(config.getBoolean("removal.drop-inventory-on-remove")),
                new PersistenceConfig(config.getBoolean("persistence.save-dummy-data")),
                new TraitDefaultConfig(
                        config.getBoolean("trait-defaults.collidable"),
                        config.getBoolean("trait-defaults.invulnerable"),
                        config.getBoolean("trait-defaults.auto-restock"),
                        config.getBoolean("trait-defaults.allow-inventory-open"),
                        config.getBoolean("trait-defaults.allow-interaction-configure")
                )
        );
    }

    private static int requirePositive(int value, String path) {
        if (value <= 0) {
            throw new IllegalStateException(path + " must be greater than 0");
        }
        return value;
    }

    private static String requireNonBlank(String value, String path) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(path + " must not be blank");
        }
        return value;
    }

    private static String requireTemplate(String value, String path) {
        String template = requireNonBlank(value, path);
        if (!template.contains("{player}") || !template.contains("{index}")) {
            throw new IllegalStateException(path + " must contain {player} and {index}");
        }
        return template;
    }
}
