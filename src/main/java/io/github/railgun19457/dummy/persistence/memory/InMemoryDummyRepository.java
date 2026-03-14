package io.github.railgun19457.dummy.persistence.memory;

import io.github.railgun19457.dummy.core.model.DummyDefinition;
import io.github.railgun19457.dummy.core.model.DummyId;
import io.github.railgun19457.dummy.core.model.DummyInventoryState;
import io.github.railgun19457.dummy.core.model.DummyRuntimeState;
import io.github.railgun19457.dummy.core.model.DummyTraitSet;
import io.github.railgun19457.dummy.core.model.SkinProfile;
import io.github.railgun19457.dummy.core.model.WorldPosition;
import io.github.railgun19457.dummy.core.port.DummyRepository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryDummyRepository implements DummyRepository {

    private final Map<DummyId, DummyDefinition> storage = new ConcurrentHashMap<>();
    private final Map<DummyId, DummyRuntimeState> runtimeSnapshots = new ConcurrentHashMap<>();
    private final Map<DummyId, Integer> storedExperience = new ConcurrentHashMap<>();

    @Override
    public void save(DummyDefinition definition) {
        storage.put(definition.id(), definition);
    }

    @Override
    public Optional<DummyDefinition> findById(DummyId dummyId) {
        return Optional.ofNullable(storage.get(dummyId));
    }

    @Override
    public List<DummyDefinition> findByOwner(UUID ownerId) {
        return storage.values().stream()
                .filter(definition -> definition.ownerId().equals(ownerId))
                .sorted(Comparator.comparing(DummyDefinition::createdAt))
                .toList();
    }

    @Override
    public List<DummyDefinition> findAll() {
        return storage.values().stream()
                .sorted(Comparator.comparing(DummyDefinition::createdAt))
                .toList();
    }

    @Override
    public void delete(DummyId dummyId) {
        storage.remove(dummyId);
        runtimeSnapshots.remove(dummyId);
        storedExperience.remove(dummyId);
    }

    @Override
    public void updateTraits(DummyId dummyId, DummyTraitSet traitSet) {
        storage.computeIfPresent(dummyId, (ignored, definition) -> new DummyDefinition(
                definition.id(),
                definition.name(),
                definition.ownerId(),
                definition.ownerName(),
                definition.lastKnownPosition(),
                definition.skinProfile(),
                traitSet,
                definition.inventoryState(),
                definition.createdAt(),
                Instant.now()
        ));
    }

    @Override
    public void updateLocation(DummyId dummyId, WorldPosition worldPosition) {
        storage.computeIfPresent(dummyId, (ignored, definition) -> new DummyDefinition(
                definition.id(),
                definition.name(),
                definition.ownerId(),
                definition.ownerName(),
                worldPosition,
                definition.skinProfile(),
                definition.traitSet(),
                definition.inventoryState(),
                definition.createdAt(),
                Instant.now()
        ));
    }

    @Override
    public void updateSkin(DummyId dummyId, SkinProfile skinProfile) {
        storage.computeIfPresent(dummyId, (ignored, definition) -> new DummyDefinition(
                definition.id(),
                definition.name(),
                definition.ownerId(),
                definition.ownerName(),
                definition.lastKnownPosition(),
                skinProfile,
                definition.traitSet(),
                definition.inventoryState(),
                definition.createdAt(),
                Instant.now()
        ));
    }

    @Override
    public void updateInventory(DummyId dummyId, DummyInventoryState inventoryState) {
        storage.computeIfPresent(dummyId, (ignored, definition) -> new DummyDefinition(
                definition.id(),
                definition.name(),
                definition.ownerId(),
                definition.ownerName(),
                definition.lastKnownPosition(),
                definition.skinProfile(),
                definition.traitSet(),
                inventoryState,
                definition.createdAt(),
                Instant.now()
        ));
    }

    @Override
    public void saveRuntimeState(DummyRuntimeState runtimeState) {
        runtimeSnapshots.put(runtimeState.dummyId(), runtimeState);
    }

    @Override
    public Map<DummyId, DummyRuntimeState> findAllRuntimeStates() {
        return Map.copyOf(runtimeSnapshots);
    }

    @Override
    public Map<DummyId, Integer> findAllStoredExperience() {
        return Map.copyOf(storedExperience);
    }

    @Override
    public void saveStoredExperience(DummyId dummyId, int amount) {
        storedExperience.put(dummyId, amount);
    }

    @Override
    public void deleteStoredExperience(DummyId dummyId) {
        storedExperience.remove(dummyId);
    }

    @Override
    public void deleteRuntimeState(DummyId dummyId) {
        runtimeSnapshots.remove(dummyId);
    }
}
