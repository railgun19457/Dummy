package io.github.railgun19457.dummy.core.action.builtin;

import io.github.railgun19457.dummy.compat.api.FakePlayerHandle;
import io.github.railgun19457.dummy.core.action.ActionRequest;
import io.github.railgun19457.dummy.core.action.ActionResult;
import io.github.railgun19457.dummy.core.action.ActionType;
import io.github.railgun19457.dummy.core.action.DummyAction;
import io.github.railgun19457.dummy.core.model.DummyDefinition;
import io.github.railgun19457.dummy.core.port.CompatBridge;
import io.github.railgun19457.dummy.core.service.DummyQueryService;
import org.bukkit.Bukkit;
import org.bukkit.World;

public final class DigAction implements DummyAction {

    private final CompatBridge compatBridge;
    private final DummyQueryService queryService;

    public DigAction(CompatBridge compatBridge, DummyQueryService queryService) {
        this.compatBridge = compatBridge;
        this.queryService = queryService;
    }

    @Override
    public ActionType type() {
        return ActionType.DIG;
    }

    @Override
    public ActionResult validate(ActionRequest request) {
        return request.argument("x") instanceof Integer
                && request.argument("y") instanceof Integer
                && request.argument("z") instanceof Integer
                ? ActionResult.success("dummy.action.dig.success")
                : ActionResult.failure("dummy.action.invalid-args");
    }

    @Override
    public ActionResult execute(ActionRequest request, FakePlayerHandle fakePlayerHandle) {
        DummyDefinition definition = queryService.findById(request.dummyId()).orElse(null);
        if (definition == null) {
            return ActionResult.failure("dummy.error.dummy-not-found");
        }
        World world = Bukkit.getWorld(definition.lastKnownPosition().worldName());
        if (world == null) {
            return ActionResult.failure("dummy.error.world-not-found");
        }
        compatBridge.startDig(fakePlayerHandle, world.getBlockAt(
                (Integer) request.argument("x"),
                (Integer) request.argument("y"),
                (Integer) request.argument("z")
        ));
        return ActionResult.success("dummy.action.dig.success");
    }
}
