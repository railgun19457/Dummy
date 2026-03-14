package io.github.railgun19457.dummy.core.action;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class DummyActionRegistry {

    private final Map<ActionType, DummyAction> actions = new EnumMap<>(ActionType.class);

    public DummyActionRegistry(Collection<DummyAction> actions) {
        for (DummyAction action : actions) {
            this.actions.put(action.type(), action);
        }
    }

    public Optional<DummyAction> find(ActionType actionType) {
        return Optional.ofNullable(actions.get(actionType));
    }
}
