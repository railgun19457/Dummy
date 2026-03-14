package io.github.railgun19457.dummy.core.port;

import io.github.railgun19457.dummy.compat.api.FakePlayerHandle;
import io.github.railgun19457.dummy.core.model.DummyDefinition;
import io.github.railgun19457.dummy.core.model.DummyId;
import io.github.railgun19457.dummy.core.model.DummyInventoryState;
import io.github.railgun19457.dummy.core.model.Rotation;
import io.github.railgun19457.dummy.core.model.SkinProfile;
import io.github.railgun19457.dummy.core.model.DummyTraitSet;
import io.github.railgun19457.dummy.core.model.WorldPosition;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public interface CompatBridge {

    FakePlayerHandle createFakePlayer(DummyDefinition definition);

    void removeFakePlayer(DummyId dummyId);

    void teleport(FakePlayerHandle fakePlayerHandle, WorldPosition worldPosition);

    void setSkin(FakePlayerHandle fakePlayerHandle, SkinProfile skinProfile);

    void attack(FakePlayerHandle fakePlayerHandle, Entity entity);

    void useItem(FakePlayerHandle fakePlayerHandle);

    void startDig(FakePlayerHandle fakePlayerHandle, Block block);

    void sleep(FakePlayerHandle fakePlayerHandle, Location location);

    void wakeUp(FakePlayerHandle fakePlayerHandle);

    void look(FakePlayerHandle fakePlayerHandle, Rotation rotation);

    void lookAtEntity(FakePlayerHandle fakePlayerHandle, Entity entity);

    void setSneaking(FakePlayerHandle fakePlayerHandle, boolean sneaking);

    void setSprinting(FakePlayerHandle fakePlayerHandle, boolean sprinting);

    void jump(FakePlayerHandle fakePlayerHandle);

    void mount(FakePlayerHandle fakePlayerHandle, Entity entity);

    void swapHands(FakePlayerHandle fakePlayerHandle);

    void holdItem(FakePlayerHandle fakePlayerHandle, ItemStack itemStack);

    void applyInventory(FakePlayerHandle fakePlayerHandle, DummyInventoryState inventoryState);

    void updateTraits(FakePlayerHandle fakePlayerHandle, DummyTraitSet traitSet);

    Optional<DummyId> resolveDummyId(Entity entity);
}
