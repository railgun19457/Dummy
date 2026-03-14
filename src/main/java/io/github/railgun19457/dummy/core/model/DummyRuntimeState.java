package io.github.railgun19457.dummy.core.model;

import io.github.railgun19457.dummy.core.action.ActionType;

public record DummyRuntimeState(
        DummyId dummyId,
        boolean spawned,
        Integer entityId,
        ActionType currentAction,
        boolean sleeping,
        boolean sneaking,
        boolean sprinting,
        Integer mountedTarget
) {
    public static DummyRuntimeState initial(DummyId dummyId) {
        return new DummyRuntimeState(dummyId, false, null, null, false, false, false, null);
    }
}
