package github.com.railgun19457.dummy.common.config;

import org.bukkit.configuration.file.FileConfiguration;

public record PluginConfig(
        int globalLimit,
        int perPlayerLimit,
        String dummyNamePrefix,
        String defaultNameTemplate,
        long skinCacheTtlMinutes,
        boolean followSleep,
        boolean dropInventoryOnRemove,
        boolean saveData,
        boolean removeOnDeath,
        boolean instantRespawnOnDeath,
        boolean defaultCollision,
        boolean defaultInvulnerable,
        boolean defaultAutoRestock,
        boolean defaultAutoFishing,
        java.util.List<String> autoCommands
) {

    public static PluginConfig from(FileConfiguration config) {
        int globalLimit = Math.max(1, config.getInt("limits.global", 100));
        int perPlayerLimit = Math.max(1, config.getInt("limits.per-player", 5));
        String dummyNamePrefix = sanitizePrefix(config.getString("dummy.name-prefix", "dm_"));
        String defaultNameTemplate = config.getString("dummy.default-name-template", "%player%_dummy");
        long skinCacheTtlMinutes = Math.max(1, config.getLong("dummy.skin-cache-ttl-minutes", 60));
        boolean followSleep = config.getBoolean("features.follow-sleep", true);
        boolean dropInventoryOnRemove = config.getBoolean("features.drop-inventory-on-remove", false);
        boolean saveData = config.getBoolean("features.save-data", true);
        boolean removeOnDeath = config.getBoolean("features.remove-on-death", true);
        boolean instantRespawnOnDeath = config.getBoolean("features.instant-respawn-on-death", true);
        boolean defaultCollision = config.getBoolean("defaults.collision", true);
        boolean defaultInvulnerable = config.getBoolean("defaults.invulnerable", false);
        boolean defaultAutoRestock = config.getBoolean("defaults.auto-restock", false);
        boolean defaultAutoFishing = config.getBoolean("defaults.auto-fishing", false);
        java.util.List<String> autoCommands = config.getStringList("dummy.auto-commands");
        return new PluginConfig(
            globalLimit,
            perPlayerLimit,
            dummyNamePrefix,
            defaultNameTemplate,
            skinCacheTtlMinutes,
            followSleep,
            dropInventoryOnRemove,
            saveData,
            removeOnDeath,
            instantRespawnOnDeath,
            defaultCollision,
            defaultInvulnerable,
            defaultAutoRestock,
            defaultAutoFishing,
            autoCommands
        );
    }

    private static String sanitizePrefix(String raw) {
        String value = raw == null ? "" : raw.replaceAll("[^A-Za-z0-9_]", "_").trim();
        if (value.length() > 10) {
            value = value.substring(0, 10);
        }
        return value;
    }
}
