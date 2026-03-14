package io.github.railgun19457.dummy.compat.api;

import io.github.railgun19457.dummy.core.model.DummyId;

import java.util.UUID;

public record SimpleFakePlayerHandle(DummyId dummyId, int entityId, UUID entityUuid) implements FakePlayerHandle {
}
