package io.github.railgun19457.dummy.core.port;

import org.bukkit.command.CommandSender;

public interface PermissionGateway {

    boolean hasPermission(CommandSender sender, String permission);
}
