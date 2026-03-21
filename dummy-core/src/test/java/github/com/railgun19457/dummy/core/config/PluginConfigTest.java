package github.com.railgun19457.dummy.core.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PluginConfigTest {

    @Test
    void shouldLoadDefaultsAndFlags() {
        FileConfiguration cfg = mock(FileConfiguration.class);
        when(cfg.getInt("limits.global", 100)).thenReturn(120);
        when(cfg.getInt("limits.per-player", 5)).thenReturn(8);
        when(cfg.getString("dummy.default-name-template", "%player%_dummy")).thenReturn("%player%_bot");
        when(cfg.getLong("dummy.skin-cache-ttl-minutes", 60)).thenReturn(90L);
        when(cfg.getBoolean("features.follow-sleep", true)).thenReturn(true);
        when(cfg.getBoolean("features.drop-inventory-on-remove", false)).thenReturn(false);
        when(cfg.getBoolean("features.save-data", true)).thenReturn(true);
        when(cfg.getBoolean("defaults.collision", true)).thenReturn(true);
        when(cfg.getBoolean("defaults.invulnerable", false)).thenReturn(false);
        when(cfg.getBoolean("defaults.auto-restock", false)).thenReturn(true);
        when(cfg.getBoolean("defaults.auto-fishing", false)).thenReturn(true);
        when(cfg.getStringList("dummy.auto-commands")).thenReturn(List.of("login %player%"));

        PluginConfig config = PluginConfig.from(cfg);

        assertEquals(120, config.globalLimit());
        assertEquals(8, config.perPlayerLimit());
        assertEquals("%player%_bot", config.defaultNameTemplate());
        assertEquals(90L, config.skinCacheTtlMinutes());
        assertEquals(1, config.autoCommands().size());
    }
}
