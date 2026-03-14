package io.github.railgun19457.dummy.core.model;

import java.util.UUID;

public record DummyId(UUID value) {

    public static DummyId random() {
        return new DummyId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
