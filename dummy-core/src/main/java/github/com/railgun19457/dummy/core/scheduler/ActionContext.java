package github.com.railgun19457.dummy.core.scheduler;

import org.bukkit.entity.Player;

public record ActionContext(
        Player operator,
        Player target,
        long createdAtMillis
) {
}
