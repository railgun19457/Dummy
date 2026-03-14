package io.github.railgun19457.dummy.core.action.builtin;

import io.github.railgun19457.dummy.compat.api.FakePlayerHandle;
import io.github.railgun19457.dummy.core.action.ActionRequest;
import io.github.railgun19457.dummy.core.action.ActionResult;
import io.github.railgun19457.dummy.core.action.ActionType;
import io.github.railgun19457.dummy.core.action.DummyAction;
import io.github.railgun19457.dummy.core.model.DummyRuntimeState;
import io.github.railgun19457.dummy.core.port.CompatBridge;
import io.github.railgun19457.dummy.core.service.DummyLifecycleService;

public final class RunAction implements DummyAction {

    private final CompatBridge compatBridge;
    private final DummyLifecycleService lifecycleService;

    public RunAction(CompatBridge compatBridge, DummyLifecycleService lifecycleService) {
        this.compatBridge = compatBridge;
        this.lifecycleService = lifecycleService;
    }

    @Override
    public ActionType type() {
        return ActionType.RUN;
    }

    @Override
    public ActionResult validate(ActionRequest request) {
        Object enabled = request.argument("enabled");
        if (!(enabled instanceof Boolean)) {
            return ActionResult.failure("dummy.action.invalid-args");
        }
        return ActionResult.success("dummy.action.run.success");
    }

    @Override
    public ActionResult execute(ActionRequest request, FakePlayerHandle fakePlayerHandle) {
        boolean enabled = (Boolean) request.argument("enabled");
        compatBridge.setSprinting(fakePlayerHandle, enabled);
        lifecycleService.updateRuntimeState(request.dummyId(), currentState -> new DummyRuntimeState(
                currentState.dummyId(),
                currentState.spawned(),
                currentState.entityId(),
                ActionType.RUN,
                currentState.sleeping(),
                currentState.sneaking(),
                enabled,
                currentState.mountedTarget()
        ));
        return ActionResult.success("dummy.action.run.success");
    }
}
