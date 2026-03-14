package io.github.railgun19457.dummy.i18n;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class MessageBundleRegistry {

    private final String fallbackLocale;
    private final Map<String, Map<String, String>> bundles;

    private MessageBundleRegistry(String fallbackLocale, Map<String, Map<String, String>> bundles) {
        this.fallbackLocale = fallbackLocale;
        this.bundles = bundles;
    }

    public static MessageBundleRegistry load(JavaPlugin plugin, String fallbackLocale) {
        File i18nDirectory = new File(plugin.getDataFolder(), "i18n");
        if (!i18nDirectory.exists() && !i18nDirectory.mkdirs()) {
            throw new IllegalStateException("Unable to create i18n directory: " + i18nDirectory.getAbsolutePath());
        }

        saveBundleIfAbsent(plugin, "zh_CN.yml");
        saveBundleIfAbsent(plugin, "en_US.yml");

        Map<String, Map<String, String>> bundles = new HashMap<>();
        loadBundle(new File(i18nDirectory, "zh_CN.yml"), "zh_CN", bundles);
        loadBundle(new File(i18nDirectory, "en_US.yml"), "en_US", bundles);

        if (!bundles.containsKey(fallbackLocale)) {
            throw new IllegalStateException("Missing fallback locale bundle: " + fallbackLocale);
        }
        return new MessageBundleRegistry(fallbackLocale, bundles);
    }

    public String getMessage(String locale, String key) {
        Map<String, String> preferredBundle = bundles.getOrDefault(locale, Collections.emptyMap());
        if (preferredBundle.containsKey(key)) {
            return preferredBundle.get(key);
        }
        Map<String, String> fallbackBundle = bundles.getOrDefault(fallbackLocale, Collections.emptyMap());
        return fallbackBundle.getOrDefault(key, key);
    }

    public Set<String> supportedLocales() {
        return Collections.unmodifiableSet(bundles.keySet());
    }

    private static void saveBundleIfAbsent(JavaPlugin plugin, String fileName) {
        File bundleFile = new File(plugin.getDataFolder(), "i18n/" + fileName);
        if (!bundleFile.exists()) {
            plugin.saveResource("i18n/" + fileName, false);
        }
    }

    private static void loadBundle(File file, String locale, Map<String, Map<String, String>> bundles) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        Map<String, String> flattened = new HashMap<>();
        flattenSection("", yaml, flattened);
        bundles.put(locale, Collections.unmodifiableMap(flattened));
    }

    private static void flattenSection(String prefix, ConfigurationSection section, Map<String, String> flattened) {
        for (String key : section.getKeys(false)) {
            String fullKey = prefix.isBlank() ? key : prefix + "." + key;
            Object value = section.get(key);
            if (value instanceof ConfigurationSection nestedSection) {
                flattenSection(fullKey, nestedSection, flattened);
                continue;
            }
            if (value != null) {
                flattened.put(fullKey, String.valueOf(value));
            }
        }
    }
}
