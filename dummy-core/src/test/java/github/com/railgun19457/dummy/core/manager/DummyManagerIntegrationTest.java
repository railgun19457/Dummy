package github.com.railgun19457.dummy.core.manager;

import github.com.railgun19457.dummy.core.config.PluginConfig;
import github.com.railgun19457.dummy.core.model.DummySession;
import github.com.railgun19457.dummy.nms.NmsBridge;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DummyManagerIntegrationTest {

    @Test
    void shouldSpawnAndRemoveByOwner() {
        AtomicInteger spawns = new AtomicInteger();
        AtomicInteger removes = new AtomicInteger();
        NmsBridge bridge = new NmsBridge() {
            @Override
            public String spawnDummy(DummySession session) {
                spawns.incrementAndGet();
                return null;
            }

            @Override
            public void removeDummy(DummySession session) {
                removes.incrementAndGet();
            }
        };

        PluginConfig config = new PluginConfig(
            100, 5, "dm_", "%player%_dummy", 60,
            true, false, true, true, true,
                true, false, false, false,
                List.of()
        );

        DummyManager manager = new DummyManager(config, new DummyRegistry(), bridge);

        Player owner = mock(Player.class);
        UUID ownerId = UUID.randomUUID();
        when(owner.getUniqueId()).thenReturn(ownerId);
        when(owner.getName()).thenReturn("owner");
        when(owner.hasPermission("dummy.admin")).thenReturn(false);
        when(owner.getLocation()).thenReturn(new org.bukkit.Location(null, 0, 64, 0));
        PlayerInventory ownerInventory = mock(PlayerInventory.class);
        when(ownerInventory.getContents()).thenReturn(new org.bukkit.inventory.ItemStack[0]);
        when(owner.getInventory()).thenReturn(ownerInventory);

        DummyManager.OperationResult spawned = manager.spawn(owner, "abc");
        assertTrue(spawned.success());
        assertTrue(spawns.get() == 1);

        DummyManager.OperationResult removed = manager.remove(owner, spawned.session().name());
        assertTrue(removed.success());
        assertTrue(removes.get() == 1);
    }

    @Test
    void shouldBlockNonOwnerWithoutAdminPermission() {
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
            100, 5, "dm_", "%player%_dummy", 60,
            true, false, true, true, true,
                true, false, false, false,
                List.of()
        );

        DummyManager manager = new DummyManager(config, new DummyRegistry(), bridge);

        Player owner = mock(Player.class);
        UUID ownerId = UUID.randomUUID();
        when(owner.getUniqueId()).thenReturn(ownerId);
        when(owner.getName()).thenReturn("owner");
        when(owner.hasPermission("dummy.admin")).thenReturn(false);
        when(owner.getLocation()).thenReturn(new org.bukkit.Location(null, 0, 64, 0));
        PlayerInventory ownerInventory = mock(PlayerInventory.class);
        when(ownerInventory.getContents()).thenReturn(new org.bukkit.inventory.ItemStack[0]);
        when(owner.getInventory()).thenReturn(ownerInventory);

        DummyManager.OperationResult spawned = manager.spawn(owner, "abc");
        assertTrue(spawned.success());

        Player other = mock(Player.class);
        when(other.getUniqueId()).thenReturn(UUID.randomUUID());
        when(other.hasPermission("dummy.admin")).thenReturn(false);

        DummyManager.OperationResult result = manager.remove(other, spawned.session().name());
        assertFalse(result.success());
    }

    @Test
    void shouldRespawnWhenRestoreSessionsEnabled() {
        AtomicInteger spawns = new AtomicInteger();
        NmsBridge bridge = new NmsBridge() {
            @Override
            public String spawnDummy(DummySession session) {
                spawns.incrementAndGet();
                return null;
            }

            @Override
            public void removeDummy(DummySession session) {
            }
        };

        PluginConfig config = new PluginConfig(
            100, 5, "dm_", "%player%_dummy", 60,
            true, false, true, true, true,
                true, false, false, false,
                List.of()
        );

        DummyManager manager = new DummyManager(config, new DummyRegistry(), bridge);
        DummySession session = new DummySession(
                UUID.randomUUID(),
                "restored",
                UUID.randomUUID(),
                "owner",
                "owner",
                new github.com.railgun19457.dummy.core.model.DummyTraits(true, false, false, false),
                new github.com.railgun19457.dummy.core.model.DummyPosition("world", 0, 64, 0, 0, 0),
                "",
                Instant.now()
        );

        manager.restoreSessions(List.of(session), true);
        assertTrue(spawns.get() == 1);
        assertTrue(manager.findByName("restored").isPresent());
    }
}
