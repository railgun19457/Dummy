package io.github.railgun19457.dummy.core.action.builtin;

import io.github.railgun19457.dummy.compat.api.FakePlayerHandle;
import io.github.railgun19457.dummy.core.action.ActionRequest;
import io.github.railgun19457.dummy.core.action.ActionResult;
import io.github.railgun19457.dummy.core.action.ActionType;
import io.github.railgun19457.dummy.core.action.DummyAction;
import io.github.railgun19457.dummy.core.port.CompatBridge;

public final class UseAction implements DummyAction {

    private final CompatBridge compatBridge;

    public UseAction(CompatBridge compatBridge) {
        this.compatBridge = compatBridge;
    }

    @Override
    public ActionType type() {
        return ActionType.USE;
    }

    @Override
    public ActionResult validate(ActionRequest request) {
        return ActionResult.success("dummy.action.use.success");
    }

    @Override
    public ActionResult execute(ActionRequest request, FakePlayerHandle fakePlayerHandle) {
        compatBridge.useItem(fakePlayerHandle);
        return ActionResult.success("dummy.action.use.success");
    }
}
