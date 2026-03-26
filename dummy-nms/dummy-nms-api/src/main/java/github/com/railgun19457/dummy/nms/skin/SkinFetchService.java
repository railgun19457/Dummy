package github.com.railgun19457.dummy.nms.skin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import github.com.railgun19457.dummy.common.log.PluginLogger;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SkinFetchService {

    private static final String USER_PROFILE_API = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String SESSION_PROFILE_API = "https://sessionserver.mojang.com/session/minecraft/profile/";

    private final PluginLogger logger;
    private final HttpClient httpClient;
    private final ExecutorService executor;
    private final Map<String, SkinTexture> cache;
    private final Set<String> inFlight;
    private final long ttlMillis;
    private final int maxCacheSize = 512;

    public SkinFetchService(PluginLogger logger, Duration ttl) {
        this.logger = logger;
        this.executor = Executors.newFixedThreadPool(2, task -> {
            Thread thread = new Thread(task, "dummy-skin-fetch");
            thread.setDaemon(true);
            return thread;
        });
        this.httpClient = HttpClient.newBuilder()
                .executor(this.executor)
                .connectTimeout(Duration.ofSeconds(4))
                .build();
        this.cache = new ConcurrentHashMap<>();
        this.inFlight = ConcurrentHashMap.newKeySet();
        this.ttlMillis = ttl.toMillis();
    }

    public Optional<SkinTexture> getCached(String playerName) {
        cleanupExpiredIfNeeded();

        String key = normalize(playerName);
        SkinTexture texture = cache.get(key);
        if (texture == null) {
            return Optional.empty();
        }

        long now = System.currentTimeMillis();
        if (now - texture.fetchedAtMillis() > ttlMillis) {
            fetchAsyncIfAbsent(playerName);
            return Optional.empty();
        }
        return Optional.of(texture);
    }

    public void fetchAsyncIfAbsent(String playerName) {
        String key = normalize(playerName);
        if (!inFlight.add(key)) {
            return;
        }

        CompletableFuture
                .supplyAsync(() -> fetch(playerName), executor)
                .whenComplete((result, throwable) -> {
                    inFlight.remove(key);

                    if (throwable != null) {
                        logger.warn("[Skin] fetch failed for " + playerName + ": " + throwable.getMessage());
                        return;
                    }

                    result.ifPresent(texture -> {
                        if (cache.size() >= maxCacheSize) {
                            evictOneOldest();
                        }
                        cache.put(key, texture);
                        logger.info("[Skin] cache updated for " + playerName);
                    });
                });
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    private Optional<SkinTexture> fetch(String playerName) {
        try {
            Optional<String> uuid = queryUuid(playerName);
            if (uuid.isEmpty()) {
                return Optional.empty();
            }
            return queryTexture(uuid.get());
        } catch (Exception exception) {
            logger.warn("[Skin] fetch error for " + playerName + ": " + exception.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> queryUuid(String playerName) throws IOException, InterruptedException {
        String encoded = URLEncoder.encode(playerName, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(USER_PROFILE_API + encoded))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 || response.body().isBlank()) {
            return Optional.empty();
        }

        JsonElement parsed = JsonParser.parseString(response.body());
        if (!parsed.isJsonObject()) {
            return Optional.empty();
        }

        JsonObject object = parsed.getAsJsonObject();
        if (!object.has("id")) {
            return Optional.empty();
        }
        return Optional.of(object.get("id").getAsString());
    }

    private Optional<SkinTexture> queryTexture(String uuid) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SESSION_PROFILE_API + uuid + "?unsigned=false"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 || response.body().isBlank()) {
            return Optional.empty();
        }

        JsonElement parsed = JsonParser.parseString(response.body());
        if (!parsed.isJsonObject()) {
            return Optional.empty();
        }

        JsonObject object = parsed.getAsJsonObject();
        JsonArray properties = object.getAsJsonArray("properties");
        if (properties == null) {
            return Optional.empty();
        }

        for (JsonElement element : properties) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject prop = element.getAsJsonObject();
            if (!prop.has("name") || !"textures".equals(prop.get("name").getAsString())) {
                continue;
            }

            if (!prop.has("value") || !prop.has("signature")) {
                continue;
            }

            return Optional.of(new SkinTexture(
                    prop.get("value").getAsString(),
                    prop.get("signature").getAsString(),
                    System.currentTimeMillis()
            ));
        }

        return Optional.empty();
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private void cleanupExpiredIfNeeded() {
        if (cache.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        cache.entrySet().removeIf(entry -> now - entry.getValue().fetchedAtMillis() > ttlMillis * 2);
    }

    private void evictOneOldest() {
        String oldestKey = null;
        long oldestTs = Long.MAX_VALUE;
        for (Map.Entry<String, SkinTexture> entry : cache.entrySet()) {
            if (entry.getValue().fetchedAtMillis() < oldestTs) {
                oldestTs = entry.getValue().fetchedAtMillis();
                oldestKey = entry.getKey();
            }
        }
        if (oldestKey != null) {
            cache.remove(oldestKey);
        }
    }
}
