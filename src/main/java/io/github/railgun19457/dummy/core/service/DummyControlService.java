package io.github.railgun19457.dummy.core.service;

import io.github.railgun19457.dummy.core.action.ActionRequest;
import io.github.railgun19457.dummy.core.action.ActionResult;
import io.github.railgun19457.dummy.core.action.ActionType;
import io.github.railgun19457.dummy.core.action.DummyAction;
import io.github.railgun19457.dummy.core.action.DummyActionRegistry;
import io.github.railgun19457.dummy.core.model.DummyRuntimeState;

import java.util.Optional;

public final class DummyControlService {

    private final DummyActionRegistry actionRegistry;
    private final DummyLifecycleService lifecycleService;

    public DummyControlService(DummyActionRegistry actionRegistry, DummyLifecycleService lifecycleService) {
        this.actionRegistry = actionRegistry;
        this.lifecycleService = lifecycleService;
    }

    public ActionResult execute(ActionType actionType, ActionRequest request) {
        Optional<DummyAction> actionOptional = actionRegistry.find(actionType);
        if (actionOptional.isEmpty()) {
            return ActionResult.failure("dummy.action.unsupported");
        }
        Optional<io.github.railgun19457.dummy.compat.api.FakePlayerHandle> fakePlayerHandleOptional = lifecycleService.resolveHandle(request.dummyId());
        if (fakePlayerHandleOptional.isEmpty()) {
            return ActionResult.failure("dummy.error.dummy-not-found");
        }
        DummyAction action = actionOptional.get();
        ActionResult validation = action.validate(request);
        if (!validation.success()) {
            return validation;
        }
        ActionResult result = action.execute(request, fakePlayerHandleOptional.get());
        if (result.success()) {
            lifecycleService.updateRuntimeState(request.dummyId(), currentState -> new DummyRuntimeState(
                    currentState.dummyId(),
                    true,
                    fakePlayerHandleOptional.get().entityId(),
                    actionType,
                    currentState.sleeping(),
                    currentState.sneaking(),
                    currentState.sprinting(),
                    currentState.mountedTarget()
            ));
        }
        return result;
    }
}
