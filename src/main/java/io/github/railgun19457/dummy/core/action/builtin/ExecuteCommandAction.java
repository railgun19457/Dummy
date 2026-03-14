package io.github.railgun19457.dummy.core.action.builtin;

import io.github.railgun19457.dummy.compat.api.FakePlayerHandle;
import io.github.railgun19457.dummy.core.action.ActionRequest;
import io.github.railgun19457.dummy.core.action.ActionResult;
import io.github.railgun19457.dummy.core.action.ActionType;
import io.github.railgun19457.dummy.core.action.DummyAction;
import io.github.railgun19457.dummy.core.service.DummyQueryService;
import org.bukkit.Bukkit;

public final class ExecuteCommandAction implements DummyAction {

    private final DummyQueryService queryService;

    public ExecuteCommandAction(DummyQueryService queryService) {
        this.queryService = queryService;
    }

    @Override
    public ActionType type() {
        return ActionType.EXECUTE_COMMAND;
    }

    @Override
    public ActionResult validate(ActionRequest request) {
        return request.argument("command") instanceof String command && !command.isBlank()
                ? ActionResult.success("dummy.action.execute_command.success")
                : ActionResult.failure("dummy.action.invalid-args");
    }

    @Override
    public ActionResult execute(ActionRequest request, FakePlayerHandle fakePlayerHandle) {
        String dummyName = queryService.findById(request.dummyId()).map(definition -> definition.name()).orElse("Dummy");
        String command = ((String) request.argument("command")).replace("{dummy}", dummyName);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        return ActionResult.success("dummy.action.execute_command.success");
    }
}
