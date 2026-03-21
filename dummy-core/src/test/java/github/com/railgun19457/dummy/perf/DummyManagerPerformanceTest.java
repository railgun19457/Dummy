package github.com.railgun19457.dummy.perf;

import github.com.railgun19457.dummy.core.config.PluginConfig;
import github.com.railgun19457.dummy.core.manager.DummyManager;
import github.com.railgun19457.dummy.core.manager.DummyRegistry;
import github.com.railgun19457.dummy.core.model.DummySession;
import github.com.railgun19457.dummy.nms.NmsBridge;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DummyManagerPerformanceTest {

    @Test
    void shouldSpawn50DummiesQuickly() {
        long elapsedMs = runSpawnBenchmark(50);
        assertTrue(elapsedMs < 2000, "spawn 50 took too long: " + elapsedMs + "ms");
    }

    @Test
    void shouldSpawn100DummiesQuickly() {
        long elapsedMs = runSpawnBenchmark(100);
        assertTrue(elapsedMs < 4000, "spawn 100 took too long: " + elapsedMs + "ms");
    }

    private long runSpawnBenchmark(int count) {
        NmsBridge bridge = new NmsBridge() {
            @Override
            public String spawnDummy(DummySession session) {
                return null;
            }

            @Override
            public void removeDummy(DummySession session) {
            }
        };

        PluginConfig config = new PluginConfig(
                count + 10,
                count + 10,
            "dm_",
                "%player%_dummy",
                60,
                true,
                false,
                true,
            true,
            true,
                true,
                false,
                false,
                false,
                List.of()
        );

        DummyManager manager = new DummyManager(config, new DummyRegistry(), bridge);

        Instant start = Instant.now();
        for (int i = 0; i < count; i++) {
            Player owner = mock(Player.class);
            PlayerInventory inv = mock(PlayerInventory.class);
            when(inv.getContents()).thenReturn(new org.bukkit.inventory.ItemStack[0]);
            when(owner.getInventory()).thenReturn(inv);
            when(owner.getLocation()).thenReturn(new Location(null, i, 64, i));
            when(owner.getName()).thenReturn("owner_" + i);
            when(owner.getUniqueId()).thenReturn(UUID.randomUUID());
            when(owner.hasPermission("dummy.admin")).thenReturn(true);
            manager.spawn(owner, "bot_" + i);
        }
        return Duration.between(start, Instant.now()).toMillis();
    }
}
