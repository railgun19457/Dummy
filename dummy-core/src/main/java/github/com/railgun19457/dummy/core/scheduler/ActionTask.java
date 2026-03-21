package github.com.railgun19457.dummy.core.scheduler;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface ActionTask {

    void run(Player player);
}
