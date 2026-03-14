package io.github.railgun19457.dummy.core.action.builtin;

import io.github.railgun19457.dummy.compat.api.FakePlayerHandle;
import io.github.railgun19457.dummy.core.action.ActionRequest;
import io.github.railgun19457.dummy.core.action.ActionResult;
import io.github.railgun19457.dummy.core.action.ActionType;
import io.github.railgun19457.dummy.core.action.DummyAction;
import io.github.railgun19457.dummy.core.port.CompatBridge;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class LookAtEntityAction implements DummyAction {

    private final CompatBridge compatBridge;

    public LookAtEntityAction(CompatBridge compatBridge) {
        this.compatBridge = compatBridge;
    }

    @Override
    public ActionType type() {
        return ActionType.LOOK_AT_ENTITY;
    }

    @Override
    public ActionResult validate(ActionRequest request) {
        return request.argument("target") instanceof String
                ? ActionResult.success("dummy.action.look_at_entity.success")
                : ActionResult.failure("dummy.action.invalid-args");
    }

    @Override
    public ActionResult execute(ActionRequest request, FakePlayerHandle fakePlayerHandle) {
        Entity target = resolveEntity((String) request.argument("target"));
        if (target == null) {
            return ActionResult.failure("dummy.action.target-not-found");
        }
        compatBridge.lookAtEntity(fakePlayerHandle, target);
        return ActionResult.success("dummy.action.look_at_entity.success");
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
