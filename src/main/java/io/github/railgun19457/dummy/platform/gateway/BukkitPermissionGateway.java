package io.github.railgun19457.dummy.platform.gateway;

import io.github.railgun19457.dummy.core.port.PermissionGateway;
import org.bukkit.command.CommandSender;

public final class BukkitPermissionGateway implements PermissionGateway {

    @Override
    public boolean hasPermission(CommandSender sender, String permission) {
        return sender.hasPermission(permission);
    }
}
