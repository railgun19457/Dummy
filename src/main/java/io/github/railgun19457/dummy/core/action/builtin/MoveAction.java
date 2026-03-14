package io.github.railgun19457.dummy.core.action.builtin;

import io.github.railgun19457.dummy.compat.api.FakePlayerHandle;
import io.github.railgun19457.dummy.core.action.ActionRequest;
import io.github.railgun19457.dummy.core.action.ActionResult;
import io.github.railgun19457.dummy.core.action.ActionType;
import io.github.railgun19457.dummy.core.action.DummyAction;
import io.github.railgun19457.dummy.core.model.DummyDefinition;
import io.github.railgun19457.dummy.core.model.Rotation;
import io.github.railgun19457.dummy.core.model.WorldPosition;
import io.github.railgun19457.dummy.core.service.DummyLifecycleService;
import io.github.railgun19457.dummy.core.service.DummyQueryService;

public final class MoveAction implements DummyAction {

    private final DummyLifecycleService lifecycleService;
    private final DummyQueryService queryService;

    public MoveAction(DummyLifecycleService lifecycleService, DummyQueryService queryService) {
        this.lifecycleService = lifecycleService;
        this.queryService = queryService;
    }

    @Override
    public ActionType type() {
        return ActionType.MOVE;
    }

    @Override
    public ActionResult validate(ActionRequest request) {
        return request.argument("x") instanceof Double
                && request.argument("y") instanceof Double
                && request.argument("z") instanceof Double
                ? ActionResult.success("dummy.action.move.success")
                : ActionResult.failure("dummy.action.invalid-args");
    }

    @Override
    public ActionResult execute(ActionRequest request, FakePlayerHandle fakePlayerHandle) {
        DummyDefinition definition = queryService.findById(request.dummyId()).orElse(null);
        if (definition == null) {
            return ActionResult.failure("dummy.error.dummy-not-found");
        }
        float yaw = request.argument("yaw") instanceof Float providedYaw ? providedYaw : definition.lastKnownPosition().rotation().yaw();
        float pitch = request.argument("pitch") instanceof Float providedPitch ? providedPitch : definition.lastKnownPosition().rotation().pitch();
        lifecycleService.teleport(request.dummyId(), new WorldPosition(
                definition.lastKnownPosition().worldName(),
                (Double) request.argument("x"),
                (Double) request.argument("y"),
                (Double) request.argument("z"),
                new Rotation(yaw, pitch)
        ));
        return ActionResult.success("dummy.action.move.success");
    }
}
