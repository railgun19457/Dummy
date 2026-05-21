package dev.dummy.skin;

import com.destroystokyo.paper.ClientOption;
import com.destroystokyo.paper.profile.ProfileProperty;
import dev.dummy.DummyPlugin;
import dev.dummy.dummy.DummySkin;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public final class SkinService {
    private static final long DEFAULT_CACHE_EXPIRES_HOURS = 24L;

    private final DummyPlugin plugin;
    private final File file;
    private final Map<String, DummySkin> cache = new ConcurrentHashMap<>();

    public SkinService(DummyPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "skins.yml");
        load();
    }

    public CompletableFuture<DummySkin> fetchPlayerSkin(String playerName) {
        String key = normalize(playerName);
        DummySkin cached = cache.get(key);
        if (cached != null && cached.hasTexture() && !isExpired(cached)) {
            return CompletableFuture.completedFuture(cached);
        }

        Player online = Bukkit.getPlayerExact(playerName);
        if (online != null && online.isOnline()) {
            DummySkin skin = skinFromPlayer(online);
            if (skin.hasTexture()) {
                cache.put(key, skin);
                save();
                return CompletableFuture.completedFuture(skin);
            }
        }

        return Bukkit.createProfile(playerName).update().thenApply(profile -> {
            ProfileProperty property = profile.getProperties()
                    .stream()
                    .filter(candidate -> candidate.getName().equals("textures"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No skin texture found for player: " + playerName));
            DummySkin skin = DummySkin.player(playerName, property.getValue(), property.getSignature());
            cache.put(key, skin);
            save();
            return skin;
        });
    }

    public DummySkin skinFromPlayer(Player player) {
        int modelParts = player.getClientOption(ClientOption.SKIN_PARTS).getRaw();
        return player.getPlayerProfile()
                .getProperties()
                .stream()
                .filter(candidate -> candidate.getName().equals("textures"))
                .findFirst()
                .map(property -> DummySkin.player(player.getName(), property.getValue(), property.getSignature(), modelParts))
                .orElse(DummySkin.NONE);
    }

    private void load() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection("skins");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            cache.put(key, DummySkin.fromConfig(root.getConfigurationSection(key)));
        }
    }

    private synchronized void save() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection root = config.createSection("skins");
        for (Map.Entry<String, DummySkin> entry : cache.entrySet()) {
            DummySkin skin = entry.getValue();
            ConfigurationSection section = root.createSection(entry.getKey());
            section.set("type", skin.type());
            section.set("value", skin.value());
            section.set("signature", skin.signature());
            section.set("model-parts", skin.modelParts());
            section.set("fetched-at", skin.fetchedAt());
        }
        try {
            config.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to save skins.yml", ex);
        }
    }

    private String normalize(String playerName) {
        return playerName.toLowerCase(Locale.ROOT);
    }

    private boolean isExpired(DummySkin skin) {
        long expiresHours = plugin.getConfig().getLong("skins.cache-expires-hours", DEFAULT_CACHE_EXPIRES_HOURS);
        if (expiresHours < 0L) {
            return false;
        }
        if (skin.fetchedAt() <= 0L) {
            return true;
        }
        long maxAgeMillis = expiresHours * 60L * 60L * 1000L;
        return System.currentTimeMillis() - skin.fetchedAt() > maxAgeMillis;
    }
}
