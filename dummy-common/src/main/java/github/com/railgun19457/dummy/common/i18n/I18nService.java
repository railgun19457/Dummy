package github.com.railgun19457.dummy.common.i18n;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.Plugin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class I18nService {
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, String> messages;

    public I18nService(Plugin plugin, String locale) {
        Map<String, String> fallback = loadLocale(plugin, "en_US");
        Map<String, String> selected = loadLocale(plugin, normalizeLocale(locale));
        if (selected.isEmpty()) {
            selected = fallback;
        }
        Map<String, String> merged = new HashMap<>(fallback);
        merged.putAll(selected);
        this.messages = Collections.unmodifiableMap(merged);
    }

    public Component message(String key) {
        return miniMessage.deserialize(messages.getOrDefault(key, "<red>[Dummy] Missing message key: " + key + "</red>"));
    }

    public Component message(String key, Map<String, String> placeholders) {
        String raw = messages.getOrDefault(key, "<red>[Dummy] Missing message key: " + key + "</red>");
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace("<" + entry.getKey() + ">", entry.getValue());
        }
        return miniMessage.deserialize(raw);
    }

    private Map<String, String> loadLocale(Plugin plugin, String locale) {
        String path = "lang/" + locale + ".json";
        try (InputStream in = plugin.getResource(path)) {
            if (in == null) {
                return Map.of();
            }
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                Map<String, String> parsed = GSON.fromJson(reader, MAP_TYPE);
                return parsed == null ? Map.of() : parsed;
            }
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private String normalizeLocale(String locale) {
        if (locale == null || locale.isBlank()) {
            return "en_US";
        }
        return locale.replace('-', '_');
    }
}
