package io.github.railgun19457.dummy.core.action.builtin;

import io.github.railgun19457.dummy.compat.api.FakePlayerHandle;
import io.github.railgun19457.dummy.core.action.ActionRequest;
import io.github.railgun19457.dummy.core.action.ActionResult;
import io.github.railgun19457.dummy.core.action.ActionType;
import io.github.railgun19457.dummy.core.action.DummyAction;
import io.github.railgun19457.dummy.core.model.DummyRuntimeState;
import io.github.railgun19457.dummy.core.port.CompatBridge;
import io.github.railgun19457.dummy.core.service.DummyLifecycleService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class RideAction implements DummyAction {

    private final CompatBridge compatBridge;
    private final DummyLifecycleService lifecycleService;

    public RideAction(CompatBridge compatBridge, DummyLifecycleService lifecycleService) {
        this.compatBridge = compatBridge;
        this.lifecycleService = lifecycleService;
    }

    @Override
    public ActionType type() {
        return ActionType.RIDE;
    }

    @Override
    public ActionResult validate(ActionRequest request) {
        return request.argument("target") instanceof String
                ? ActionResult.success("dummy.action.ride.success")
                : ActionResult.failure("dummy.action.invalid-args");
    }

    @Override
    public ActionResult execute(ActionRequest request, FakePlayerHandle fakePlayerHandle) {
        Entity target = resolveEntity((String) request.argument("target"));
        if (target == null) {
            return ActionResult.failure("dummy.action.target-not-found");
        }
        compatBridge.mount(fakePlayerHandle, target);
        lifecycleService.updateRuntimeState(request.dummyId(), currentState -> new DummyRuntimeState(
                currentState.dummyId(),
                currentState.spawned(),
                currentState.entityId(),
                ActionType.RIDE,
                currentState.sleeping(),
                currentState.sneaking(),
                currentState.sprinting(),
                target.getEntityId()
        ));
        return ActionResult.success("dummy.action.ride.success");
    }

    private Entity resolveEntity(String token) {
        Player player = Bukkit.getPlayerExact(token);
        if (player != null) {
            return player;
        }
        try {
            return Bukkit.getEntity(UUID.fromString(token));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
