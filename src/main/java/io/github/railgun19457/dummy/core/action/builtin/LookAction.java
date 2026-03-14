package io.github.railgun19457.dummy.core.action.builtin;

import io.github.railgun19457.dummy.compat.api.FakePlayerHandle;
import io.github.railgun19457.dummy.core.action.ActionRequest;
import io.github.railgun19457.dummy.core.action.ActionResult;
import io.github.railgun19457.dummy.core.action.ActionType;
import io.github.railgun19457.dummy.core.action.DummyAction;
import io.github.railgun19457.dummy.core.model.Rotation;
import io.github.railgun19457.dummy.core.port.CompatBridge;

public final class LookAction implements DummyAction {

    private final CompatBridge compatBridge;

    public LookAction(CompatBridge compatBridge) {
        this.compatBridge = compatBridge;
    }

    @Override
    public ActionType type() {
        return ActionType.LOOK;
    }

    @Override
    public ActionResult validate(ActionRequest request) {
        Object yaw = request.argument("yaw");
        Object pitch = request.argument("pitch");
        if (!(yaw instanceof Float) || !(pitch instanceof Float)) {
            return ActionResult.failure("dummy.action.invalid-args");
        }
        return ActionResult.success("dummy.action.look.success");
    }

    @Override
    public ActionResult execute(ActionRequest request, FakePlayerHandle fakePlayerHandle) {
        float yaw = (Float) request.argument("yaw");
        float pitch = (Float) request.argument("pitch");
        compatBridge.look(fakePlayerHandle, new Rotation(yaw, pitch));
        return ActionResult.success("dummy.action.look.success");
    }
}
