package io.github.railgun19457.dummy.core.model;

import java.time.Instant;
import java.util.UUID;

public record DummyDefinition(
        DummyId id,
        String name,
        UUID ownerId,
        String ownerName,
        WorldPosition lastKnownPosition,
        SkinProfile skinProfile,
        DummyTraitSet traitSet,
        DummyInventoryState inventoryState,
        Instant createdAt,
        Instant updatedAt
) {
    public DummyDefinition withUpdatedAt(Instant updatedAt) {
        return new DummyDefinition(
                id,
                name,
                ownerId,
                ownerName,
                lastKnownPosition,
                skinProfile,
                traitSet,
                inventoryState,
                createdAt,
                updatedAt
        );
    }
}
