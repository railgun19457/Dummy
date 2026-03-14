package io.github.railgun19457.dummy.core.action;

import io.github.railgun19457.dummy.core.model.DummyId;
import org.bukkit.command.CommandSender;

import java.util.Map;

public record ActionRequest(
        DummyId dummyId,
        CommandSender initiator,
        Map<String, Object> arguments
) {
    public Object argument(String key) {
        return arguments.get(key);
    }
}
