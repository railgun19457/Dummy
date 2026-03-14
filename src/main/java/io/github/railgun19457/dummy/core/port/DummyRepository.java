package io.github.railgun19457.dummy.core.port;

import io.github.railgun19457.dummy.core.model.DummyDefinition;
import io.github.railgun19457.dummy.core.model.DummyId;
import io.github.railgun19457.dummy.core.model.DummyInventoryState;
import io.github.railgun19457.dummy.core.model.DummyRuntimeState;
import io.github.railgun19457.dummy.core.model.DummyTraitSet;
import io.github.railgun19457.dummy.core.model.SkinProfile;
import io.github.railgun19457.dummy.core.model.WorldPosition;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface DummyRepository {

    void save(DummyDefinition definition);

    Optional<DummyDefinition> findById(DummyId dummyId);

    List<DummyDefinition> findByOwner(UUID ownerId);

    List<DummyDefinition> findAll();

    void delete(DummyId dummyId);

    void updateTraits(DummyId dummyId, DummyTraitSet traitSet);

    void updateLocation(DummyId dummyId, WorldPosition worldPosition);

    void updateSkin(DummyId dummyId, SkinProfile skinProfile);

    void updateInventory(DummyId dummyId, DummyInventoryState inventoryState);

    void saveRuntimeState(DummyRuntimeState runtimeState);

    Map<DummyId, DummyRuntimeState> findAllRuntimeStates();

    Map<DummyId, Integer> findAllStoredExperience();

    void saveStoredExperience(DummyId dummyId, int amount);

    void deleteStoredExperience(DummyId dummyId);

    void deleteRuntimeState(DummyId dummyId);
}
