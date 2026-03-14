package io.github.railgun19457.dummy.core.service;

import io.github.railgun19457.dummy.common.config.PluginConfig;
import io.github.railgun19457.dummy.common.InventoryCodec;
import io.github.railgun19457.dummy.compat.api.FakePlayerHandle;
import io.github.railgun19457.dummy.core.model.DummyDefinition;
import io.github.railgun19457.dummy.core.model.DummyId;
import io.github.railgun19457.dummy.core.model.DummyInventoryState;
import io.github.railgun19457.dummy.core.model.DummyRuntimeState;
import io.github.railgun19457.dummy.core.model.DummyTraitSet;
import io.github.railgun19457.dummy.core.model.Rotation;
import io.github.railgun19457.dummy.core.model.SkinProfile;
import io.github.railgun19457.dummy.core.model.WorldPosition;
import io.github.railgun19457.dummy.core.port.CompatBridge;
import io.github.railgun19457.dummy.core.port.DummyRepository;
import io.github.railgun19457.dummy.core.port.SkinGateway;
import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

public final class DummyLifecycleService {

    private final PluginConfig pluginConfig;
    private final DummyRepository dummyRepository;
    private final CompatBridge compatBridge;
    private final SkinGateway skinGateway;
    private final Map<DummyId, DummyRuntimeState> runtimeStates = new ConcurrentHashMap<>();
    private final Map<DummyId, FakePlayerHandle> handles = new ConcurrentHashMap<>();
    private final Map<DummyId, Integer> storedExperience = new ConcurrentHashMap<>();

    public DummyLifecycleService(
            PluginConfig pluginConfig,
            DummyRepository dummyRepository,
            CompatBridge compatBridge,
            SkinGateway skinGateway
    ) {
        this.pluginConfig = pluginConfig;
        this.dummyRepository = dummyRepository;
        this.compatBridge = compatBridge;
        this.skinGateway = skinGateway;
    }

    public CompletableFuture<DummyDefinition> spawn(Player owner, String requestedName) {
        validateSpawnLimits(owner.getUniqueId());
        String name = requestedName == null || requestedName.isBlank()
                ? defaultName(owner.getName(), dummyRepository.findByOwner(owner.getUniqueId()).size() + 1)
                : requestedName;
        Location location = owner.getLocation();
        DummyId dummyId = DummyId.random();
        Instant now = Instant.now();
        DummyDefinition definition = new DummyDefinition(
                dummyId,
                name,
                owner.getUniqueId(),
                owner.getName(),
                new WorldPosition(location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), new Rotation(location.getYaw(), location.getPitch())),
                new SkinProfile(owner.getName(), null, null),
                defaultTraits(),
                DummyInventoryState.empty(),
                now,
                now
        );
        return skinGateway.fetchByPlayerName(owner.getName()).thenApply(skinProfile -> {
            DummyDefinition resolvedDefinition = new DummyDefinition(
                    definition.id(),
                    definition.name(),
                    definition.ownerId(),
                    definition.ownerName(),
                    definition.lastKnownPosition(),
                    skinProfile,
                    definition.traitSet(),
                    definition.inventoryState(),
                    definition.createdAt(),
                    definition.updatedAt()
            );
            dummyRepository.save(resolvedDefinition);
            FakePlayerHandle handle = compatBridge.createFakePlayer(resolvedDefinition);
            handles.put(dummyId, handle);
            compatBridge.applyInventory(handle, resolvedDefinition.inventoryState());
            DummyRuntimeState runtimeState = new DummyRuntimeState(dummyId, true, handle.entityId(), null, false, false, false, null);
            runtimeStates.put(dummyId, runtimeState);
            dummyRepository.saveRuntimeState(runtimeState);
            return resolvedDefinition;
        });
    }

    public boolean remove(DummyId dummyId) {
        Optional<DummyDefinition> existing = dummyRepository.findById(dummyId);
        if (existing.isEmpty()) {
            return false;
        }
        if (pluginConfig.removal().dropInventoryOnRemove()) {
            dropInventory(existing.get());
        }
        compatBridge.removeFakePlayer(dummyId);
        handles.remove(dummyId);
        runtimeStates.remove(dummyId);
        dummyRepository.delete(dummyId);
        dummyRepository.deleteRuntimeState(dummyId);
        dummyRepository.deleteStoredExperience(dummyId);
        storedExperience.remove(dummyId);
        return true;
    }

    public Optional<DummyDefinition> teleport(DummyId dummyId, WorldPosition worldPosition) {
        Optional<DummyDefinition> definitionOptional = dummyRepository.findById(dummyId);
        definitionOptional.ifPresent(definition -> {
            FakePlayerHandle handle = handles.computeIfAbsent(dummyId, ignored -> compatBridge.createFakePlayer(definition));
            compatBridge.teleport(handle, worldPosition);
            dummyRepository.updateLocation(dummyId, worldPosition);
        });
        return definitionOptional;
    }

    public Optional<DummyDefinition> updateSkin(DummyId dummyId, String playerName) {
        Optional<DummyDefinition> definitionOptional = dummyRepository.findById(dummyId);
        definitionOptional.ifPresent(definition -> skinGateway.fetchByPlayerName(playerName).thenAccept(skinProfile -> {
            FakePlayerHandle handle = handles.computeIfAbsent(dummyId, ignored -> compatBridge.createFakePlayer(definition));
            compatBridge.setSkin(handle, skinProfile);
            dummyRepository.updateSkin(dummyId, skinProfile);
        }));
        return definitionOptional;
    }

    public Optional<DummyDefinition> updateTraits(DummyId dummyId, DummyTraitSet traitSet) {
        Optional<DummyDefinition> definitionOptional = dummyRepository.findById(dummyId);
        definitionOptional.ifPresent(definition -> {
            dummyRepository.updateTraits(dummyId, traitSet);
            resolveHandle(dummyId).ifPresent(handle -> compatBridge.updateTraits(handle, traitSet));
        });
        return definitionOptional;
    }

    public Optional<DummyDefinition> updateInventory(DummyId dummyId, DummyInventoryState inventoryState) {
        Optional<DummyDefinition> definitionOptional = dummyRepository.findById(dummyId);
        definitionOptional.ifPresent(definition -> {
            DummyInventoryState finalState = definition.traitSet().autoRestock()
                    ? mergeWithExisting(definition.inventoryState(), inventoryState)
                    : inventoryState;
            dummyRepository.updateInventory(dummyId, finalState);
            resolveHandle(dummyId).ifPresent(handle -> {
                compatBridge.applyInventory(handle, finalState);
                if (finalState.contents() != null && !finalState.contents().isEmpty()) {
                    String firstItem = finalState.contents().get(0);
                    if (firstItem != null && !firstItem.isBlank()) {
                        compatBridge.holdItem(handle, InventoryCodec.decode(firstItem));
                    }
                }
            });
        });
        return definitionOptional;
    }

    public int transferExperience(DummyId dummyId, Player player) {
        int amount = storedExperience.getOrDefault(dummyId, 0);
        if (amount > 0) {
            player.giveExp(amount);
            storedExperience.put(dummyId, 0);
            dummyRepository.saveStoredExperience(dummyId, 0);
        }
        return amount;
    }

    public void addExperience(DummyId dummyId, int amount) {
        int updated = storedExperience.merge(dummyId, amount, Integer::sum);
        dummyRepository.saveStoredExperience(dummyId, updated);
    }

    public void restoreStoredExperience(Map<DummyId, Integer> snapshot) {
        storedExperience.clear();
        storedExperience.putAll(snapshot);
    }

    public boolean followOwnerSleepEnabled() {
        return pluginConfig.sleep().followOwnerSleep();
    }

    public void setHeldItem(DummyId dummyId, ItemStack itemStack) {
        dummyRepository.findById(dummyId).ifPresent(definition -> {
            java.util.ArrayList<String> contents = new java.util.ArrayList<>(pad(definition.inventoryState().contents(), 36));
            contents.set(0, InventoryCodec.encode(itemStack));
            updateInventory(dummyId, new DummyInventoryState(contents, pad(definition.inventoryState().armorContents(), 4), definition.inventoryState().offhandItem()));
        });
    }

    public void swapHandItems(DummyId dummyId) {
        dummyRepository.findById(dummyId).ifPresent(definition -> {
            java.util.ArrayList<String> contents = new java.util.ArrayList<>(pad(definition.inventoryState().contents(), 36));
            String mainHand = contents.get(0);
            contents.set(0, definition.inventoryState().offhandItem());
            updateInventory(dummyId, new DummyInventoryState(contents, pad(definition.inventoryState().armorContents(), 4), mainHand));
        });
    }

    public void dropMainHandItem(DummyId dummyId) {
        dummyRepository.findById(dummyId).ifPresent(definition -> {
            java.util.ArrayList<String> contents = new java.util.ArrayList<>(pad(definition.inventoryState().contents(), 36));
            ItemStack mainHand = InventoryCodec.decode(contents.get(0));
            if (mainHand != null && !mainHand.getType().isAir()) {
                World world = Bukkit.getWorld(definition.lastKnownPosition().worldName());
                if (world != null) {
                    Location location = new Location(world, definition.lastKnownPosition().x(), definition.lastKnownPosition().y(), definition.lastKnownPosition().z());
                    world.dropItemNaturally(location, mainHand);
                }
            }
            contents.set(0, null);
            updateInventory(dummyId, new DummyInventoryState(contents, pad(definition.inventoryState().armorContents(), 4), definition.inventoryState().offhandItem()));
        });
    }

    public List<DummyDefinition> findAllByOwner(UUID ownerId) {
        return dummyRepository.findByOwner(ownerId);
    }

    public Optional<FakePlayerHandle> resolveHandle(DummyId dummyId) {
        FakePlayerHandle existingHandle = handles.get(dummyId);
        if (existingHandle != null) {
            return Optional.of(existingHandle);
        }
        return dummyRepository.findById(dummyId).map(definition -> {
            FakePlayerHandle handle = compatBridge.createFakePlayer(definition);
            handles.put(dummyId, handle);
            runtimeStates.putIfAbsent(dummyId, new DummyRuntimeState(dummyId, true, handle.entityId(), null, false, false, false, null));
            return handle;
        });
    }

    public Optional<DummyRuntimeState> runtimeState(DummyId dummyId) {
        return Optional.ofNullable(runtimeStates.get(dummyId));
    }

    public DummyRuntimeState updateRuntimeState(DummyId dummyId, UnaryOperator<DummyRuntimeState> updater) {
        DummyRuntimeState updatedState = runtimeStates.compute(dummyId, (ignored, currentState) -> {
            DummyRuntimeState baseState = currentState != null ? currentState : DummyRuntimeState.initial(dummyId);
            return updater.apply(baseState);
        });
        dummyRepository.saveRuntimeState(updatedState);
        return updatedState;
    }

    public void restoreRuntimeState(DummyRuntimeState runtimeState) {
        runtimeStates.put(runtimeState.dummyId(), runtimeState);
        dummyRepository.saveRuntimeState(runtimeState);
    }

    public void registerRecoveredDummy(DummyDefinition definition, DummyRuntimeState runtimeState) {
        FakePlayerHandle handle = compatBridge.createFakePlayer(definition);
        handles.put(definition.id(), handle);
        compatBridge.applyInventory(handle, definition.inventoryState());
        DummyRuntimeState recoveredState = new DummyRuntimeState(
                runtimeState.dummyId(),
                true,
                handle.entityId(),
                runtimeState.currentAction(),
                runtimeState.sleeping(),
                runtimeState.sneaking(),
                runtimeState.sprinting(),
                runtimeState.mountedTarget()
        );
        runtimeStates.put(definition.id(), recoveredState);
        dummyRepository.saveRuntimeState(recoveredState);
    }

    public void shutdownAll() {
        for (DummyRuntimeState runtimeState : runtimeStates.values()) {
            dummyRepository.saveRuntimeState(runtimeState);
        }
        for (Map.Entry<DummyId, Integer> entry : storedExperience.entrySet()) {
            dummyRepository.saveStoredExperience(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<DummyId, FakePlayerHandle> entry : handles.entrySet()) {
            compatBridge.removeFakePlayer(entry.getKey());
        }
        handles.clear();
    }

    public Map<DummyId, DummyRuntimeState> runtimeStates() {
        return Map.copyOf(runtimeStates);
    }

    private void validateSpawnLimits(UUID ownerId) {
        int serverCount = dummyRepository.findAll().size();
        if (serverCount >= pluginConfig.limits().serverMaxDummies()) {
            throw new IllegalStateException("Server dummy limit reached");
        }
        int ownerCount = dummyRepository.findByOwner(ownerId).size();
        if (ownerCount >= pluginConfig.limits().perPlayerMaxDummies()) {
            throw new IllegalStateException("Player dummy limit reached");
        }
    }

    private String defaultName(String playerName, int index) {
        return pluginConfig.naming().defaultNameTemplate()
                .replace("{player}", playerName)
                .replace("{index}", String.valueOf(index));
    }

    private DummyTraitSet defaultTraits() {
        return new DummyTraitSet(
                pluginConfig.traitDefaults().collidable(),
                pluginConfig.traitDefaults().invulnerable(),
                pluginConfig.traitDefaults().autoRestock(),
                pluginConfig.traitDefaults().allowInventoryOpen(),
                pluginConfig.traitDefaults().allowInteractionConfigure()
        );
    }

    private DummyInventoryState mergeWithExisting(DummyInventoryState existing, DummyInventoryState updated) {
        return new DummyInventoryState(
                mergeLists(existing.contents(), updated.contents(), 36),
                mergeLists(existing.armorContents(), updated.armorContents(), 4),
                updated.offhandItem() == null || updated.offhandItem().isBlank() ? existing.offhandItem() : updated.offhandItem()
        );
    }

    private List<String> mergeLists(List<String> existing, List<String> updated, int expectedSize) {
        java.util.ArrayList<String> merged = new java.util.ArrayList<>(expectedSize);
        for (int index = 0; index < expectedSize; index++) {
            String updatedValue = index < updated.size() ? updated.get(index) : null;
            String existingValue = index < existing.size() ? existing.get(index) : null;
            merged.add(updatedValue == null || updatedValue.isBlank() ? existingValue : updatedValue);
        }
        return merged;
    }

    private List<String> pad(List<String> source, int expectedSize) {
        java.util.ArrayList<String> filled = new java.util.ArrayList<>(expectedSize);
        for (int index = 0; index < expectedSize; index++) {
            filled.add(index < source.size() ? source.get(index) : null);
        }
        return filled;
    }

    private void dropInventory(DummyDefinition definition) {
        World world = Bukkit.getWorld(definition.lastKnownPosition().worldName());
        if (world == null) {
            return;
        }
        Location location = new Location(world, definition.lastKnownPosition().x(), definition.lastKnownPosition().y(), definition.lastKnownPosition().z());
        dropEncodedItems(location, definition.inventoryState().contents());
        dropEncodedItems(location, definition.inventoryState().armorContents());
        ItemStack offhand = InventoryCodec.decode(definition.inventoryState().offhandItem());
        if (offhand != null && !offhand.getType().isAir()) {
            world.dropItemNaturally(location, offhand);
        }
    }

    private void dropEncodedItems(Location location, List<String> encodedItems) {
        for (String encodedItem : encodedItems) {
            ItemStack itemStack = InventoryCodec.decode(encodedItem);
            if (itemStack != null && !itemStack.getType().isAir()) {
                location.getWorld().dropItemNaturally(location, itemStack);
            }
        }
    }
}
