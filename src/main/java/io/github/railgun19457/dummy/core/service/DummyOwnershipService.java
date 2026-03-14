package io.github.railgun19457.dummy.core.service;

import io.github.railgun19457.dummy.core.model.DummyDefinition;
import io.github.railgun19457.dummy.core.port.PermissionGateway;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class DummyOwnershipService {

    private static final String ADMIN_PERMISSION = "dummy.admin";

    private final PermissionGateway permissionGateway;

    public DummyOwnershipService(PermissionGateway permissionGateway) {
        this.permissionGateway = permissionGateway;
    }

    public boolean canAccess(CommandSender sender, DummyDefinition definition, String ownPermission, String othersPermission) {
        if (permissionGateway.hasPermission(sender, ADMIN_PERMISSION)) {
            return true;
        }
        if (!(sender instanceof Player player)) {
            return false;
        }
        if (player.getUniqueId().equals(definition.ownerId())) {
            return permissionGateway.hasPermission(sender, ownPermission);
        }
        return permissionGateway.hasPermission(sender, othersPermission);
    }
}
