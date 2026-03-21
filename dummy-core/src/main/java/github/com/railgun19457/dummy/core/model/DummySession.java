package github.com.railgun19457.dummy.core.model;

import java.time.Instant;
import java.util.UUID;

public record DummySession(
        UUID id,
        String name,
        UUID ownerUuid,
        String ownerName,
        String skinSource,
        DummyTraits traits,
        DummyPosition position,
        String inventoryData,
        Instant createdAt
) {
}
