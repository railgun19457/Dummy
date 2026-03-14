package io.github.railgun19457.dummy.core.action.builtin;

import io.github.railgun19457.dummy.compat.api.FakePlayerHandle;
import io.github.railgun19457.dummy.core.action.ActionRequest;
import io.github.railgun19457.dummy.core.action.ActionResult;
import io.github.railgun19457.dummy.core.action.ActionType;
import io.github.railgun19457.dummy.core.action.DummyAction;
import io.github.railgun19457.dummy.core.service.DummyQueryService;
import org.bukkit.Bukkit;

public final class SendChatAction implements DummyAction {

    private final DummyQueryService queryService;

    public SendChatAction(DummyQueryService queryService) {
        this.queryService = queryService;
    }

    @Override
    public ActionType type() {
        return ActionType.SEND_CHAT;
    }

    @Override
    public ActionResult validate(ActionRequest request) {
        return request.argument("message") instanceof String message && !message.isBlank()
                ? ActionResult.success("dummy.action.send_chat.success")
                : ActionResult.failure("dummy.action.invalid-args");
    }

    @Override
    public ActionResult execute(ActionRequest request, FakePlayerHandle fakePlayerHandle) {
        String dummyName = queryService.findById(request.dummyId()).map(definition -> definition.name()).orElse("Dummy");
        Bukkit.broadcastMessage("<" + dummyName + "> " + request.argument("message"));
        return ActionResult.success("dummy.action.send_chat.success");
    }
}
