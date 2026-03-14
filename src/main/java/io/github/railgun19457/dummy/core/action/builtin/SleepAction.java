package io.github.railgun19457.dummy.core.action.builtin;

import io.github.railgun19457.dummy.compat.api.FakePlayerHandle;
import io.github.railgun19457.dummy.core.action.ActionRequest;
import io.github.railgun19457.dummy.core.action.ActionResult;
import io.github.railgun19457.dummy.core.action.ActionType;
import io.github.railgun19457.dummy.core.action.DummyAction;
import io.github.railgun19457.dummy.core.model.DummyDefinition;
import io.github.railgun19457.dummy.core.model.DummyRuntimeState;
import io.github.railgun19457.dummy.core.port.CompatBridge;
import io.github.railgun19457.dummy.core.service.DummyLifecycleService;
import io.github.railgun19457.dummy.core.service.DummyQueryService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public final class SleepAction implements DummyAction {

    private final CompatBridge compatBridge;
    private final DummyLifecycleService lifecycleService;
    private final DummyQueryService queryService;

    public SleepAction(CompatBridge compatBridge, DummyLifecycleService lifecycleService, DummyQueryService queryService) {
        this.compatBridge = compatBridge;
        this.lifecycleService = lifecycleService;
        this.queryService = queryService;
    }

    @Override
    public ActionType type() {
        return ActionType.SLEEP;
    }

    @Override
    public ActionResult validate(ActionRequest request) {
        return ActionResult.success("dummy.action.sleep.success");
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
        Location location = new Location(world, definition.lastKnownPosition().x(), definition.lastKnownPosition().y(), definition.lastKnownPosition().z());
        compatBridge.sleep(fakePlayerHandle, location);
        lifecycleService.updateRuntimeState(request.dummyId(), currentState -> new DummyRuntimeState(
                currentState.dummyId(),
                currentState.spawned(),
                currentState.entityId(),
                ActionType.SLEEP,
                true,
                currentState.sneaking(),
                currentState.sprinting(),
                currentState.mountedTarget()
        ));
        return ActionResult.success("dummy.action.sleep.success");
    }
}
