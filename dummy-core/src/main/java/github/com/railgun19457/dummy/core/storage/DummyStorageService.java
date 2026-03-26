package github.com.railgun19457.dummy.core.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import github.com.railgun19457.dummy.common.log.PluginLogger;
import github.com.railgun19457.dummy.api.model.DummySession;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class DummyStorageService {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Instant.class, (com.google.gson.JsonSerializer<Instant>) (src, typeOfSrc, context) ->
                    src == null ? null : new com.google.gson.JsonPrimitive(src.toString()))
            .registerTypeAdapter(Instant.class, (com.google.gson.JsonDeserializer<Instant>) (json, typeOfT, context) -> {
                if (json == null || json.isJsonNull()) {
                    return Instant.now();
                }
                return Instant.parse(json.getAsString());
            })
            .setPrettyPrinting()
            .create();
    private static final Type FILE_TYPE = new TypeToken<StorageFile>() {}.getType();

    private final Plugin plugin;
    private final PluginLogger logger;
    private final ExecutorService ioExecutor;
    private final Path storagePath;

    public DummyStorageService(Plugin plugin, PluginLogger logger) {
        this.plugin = plugin;
        this.logger = logger;
        this.ioExecutor = new ThreadPoolExecutor(
                1,
                2,
                30L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(32),
                r -> {
                    Thread thread = new Thread(r, "dummy-storage-io");
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        this.storagePath = plugin.getDataFolder().toPath().resolve("dummies.json");
    }

    public List<DummySession> loadSessions() {
        try {
            Files.createDirectories(storagePath.getParent());
            if (!Files.exists(storagePath)) {
                return List.of();
            }
            try (Reader reader = Files.newBufferedReader(storagePath, StandardCharsets.UTF_8)) {
                StorageFile file = GSON.fromJson(reader, FILE_TYPE);
                if (file == null || file.sessions() == null) {
                    return List.of();
                }
                return file.sessions();
            }
        } catch (Exception exception) {
            logger.error("[Storage] load failed", exception);
            return List.of();
        }
    }

    public CompletableFuture<Void> saveSessionsAsync(List<DummySession> sessions) {
        return CompletableFuture.runAsync(() -> {
            try {
                Files.createDirectories(storagePath.getParent());
                Path tempPath = storagePath.resolveSibling(storagePath.getFileName() + ".tmp");
                StorageFile payload = new StorageFile(1, sessions == null ? List.of() : sessions, Map.of("plugin", plugin.getPluginMeta().getVersion()));
                try (Writer writer = Files.newBufferedWriter(tempPath, StandardCharsets.UTF_8)) {
                    GSON.toJson(payload, FILE_TYPE, writer);
                }
                Files.move(tempPath, storagePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        }, ioExecutor).whenComplete((ignored, throwable) -> {
            if (throwable != null) {
                logger.error("[Storage] save failed", throwable);
            }
        });
    }

    public void shutdown() {
        ioExecutor.shutdown();
    }

    private record StorageFile(int version, List<DummySession> sessions, Map<String, String> meta) {
    }
}
