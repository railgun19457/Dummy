package io.github.railgun19457.dummy.core.action.builtin;

import io.github.railgun19457.dummy.compat.api.FakePlayerHandle;
import io.github.railgun19457.dummy.core.action.ActionRequest;
import io.github.railgun19457.dummy.core.action.ActionResult;
import io.github.railgun19457.dummy.core.action.ActionType;
import io.github.railgun19457.dummy.core.action.DummyAction;
import io.github.railgun19457.dummy.core.port.CompatBridge;

public final class JumpAction implements DummyAction {

    private final CompatBridge compatBridge;

    public JumpAction(CompatBridge compatBridge) {
        this.compatBridge = compatBridge;
    }

    @Override
    public ActionType type() {
        return ActionType.JUMP;
    }

    @Override
    public ActionResult validate(ActionRequest request) {
        return ActionResult.success("dummy.action.jump.success");
    }

    @Override
    public ActionResult execute(ActionRequest request, FakePlayerHandle fakePlayerHandle) {
        compatBridge.jump(fakePlayerHandle);
        return ActionResult.success("dummy.action.jump.success");
    }
}
