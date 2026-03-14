package io.github.railgun19457.dummy.core.action;

import io.github.railgun19457.dummy.compat.api.FakePlayerHandle;

public interface DummyAction {

    ActionType type();

    ActionResult validate(ActionRequest request);

    ActionResult execute(ActionRequest request, FakePlayerHandle fakePlayerHandle);
}
