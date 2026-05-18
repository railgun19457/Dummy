package dev.dummy.nms.paper;

import com.destroystokyo.paper.profile.ProfileProperty;
import dev.dummy.dummy.DummySettings;
import dev.dummy.dummy.DummySkin;
import dev.dummy.nms.DummyHandle;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.scheduler.BukkitTask;

public final class PaperDummyHandle implements DummyHandle {
    private static final String NO_COLLISION_TEAM = "dummy_no_collision";

    private final ServerPlayer handle;
    private final BukkitTask tickerTask;
    private boolean removed;
    private boolean listed = true;

    public PaperDummyHandle(ServerPlayer handle, BukkitTask tickerTask) {
        this.handle = handle;
        this.tickerTask = tickerTask;
    }

    @Override
    public Player player() {
        return handle.getBukkitEntity();
    }

    @Override
    public void teleport(Location location) {
        player().teleport(location);
    }

    @Override
    public void applySettings(String name, DummySettings settings) {
        Player player = player();
        Component displayName = Component.text(settings.displayName(name));
        player.setInvulnerable(settings.invulnerable() || settings.ghost());
        player.setCollidable(settings.collision() && !settings.ghost());
        applyCollisionRule(player, settings.collision() && !settings.ghost());
        player.setGravity(!settings.ghost());
        player.setNoPhysics(settings.ghost());
        player.setInvisible(false);
        player.displayName(displayName);
        player.playerListName(displayName);
        player.customName(displayName);
        player.setCustomNameVisible(true);
        listed = settings.showInTab();
    }

    @Override
    public void applySkin(DummySkin skin) {
        Player player = player();
        com.destroystokyo.paper.profile.PlayerProfile profile = player.getPlayerProfile();
        profile.removeProperty("textures");
        if (skin.hasTexture()) {
            ProfileProperty property = skin.signature().isBlank()
                    ? new ProfileProperty("textures", skin.value())
                    : new ProfileProperty("textures", skin.value(), skin.signature());
            profile.setProperty(property);
        }
        player.setPlayerProfile(profile);
        refreshPlayerInfo();
    }

    @Override
    public void remove(Component reason) {
        if (removed) {
            return;
        }
        removed = true;
        tickerTask.cancel();
        Player player = player();
        removeCollisionRule(player);
        sendRemovePackets();
        closeConnection();
        if (player.isOnline()) {
            ((CraftServer) Bukkit.getServer()).getHandle().remove(handle, reason);
        }
        if (!handle.isRemoved()) {
            handle.discard();
        }
    }

    private void applyCollisionRule(Player player, boolean collision) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam(NO_COLLISION_TEAM);
        if (collision) {
            if (team != null) {
                team.removeEntry(player.getName());
            }
            return;
        }
        if (team == null) {
            team = scoreboard.registerNewTeam(NO_COLLISION_TEAM);
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        }
        team.addEntry(player.getName());
    }

    private void removeCollisionRule(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam(NO_COLLISION_TEAM);
        if (team != null) {
            team.removeEntry(player.getName());
        }
    }

    private void sendRemovePackets() {
        var removeInfo = new ClientboundPlayerInfoRemovePacket(List.of(handle.getUUID()));
        sendRemoveEntityPacket();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online instanceof CraftPlayer craftPlayer && !online.getUniqueId().equals(handle.getUUID())) {
                craftPlayer.getHandle().connection.send(removeInfo);
            }
        }
    }

    private void sendRemoveEntityPacket() {
        var removeEntity = new ClientboundRemoveEntitiesPacket(handle.getId());
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online instanceof CraftPlayer craftPlayer && !online.getUniqueId().equals(handle.getUUID())) {
                craftPlayer.getHandle().connection.send(removeEntity);
            }
        }
    }

    private void closeConnection() {
        if (handle.connection instanceof DummyServerGamePacketListener dummyConnection) {
            dummyConnection.closeDummyConnection();
        }
    }

    private void refreshPlayerInfo() {
        var remove = new ClientboundPlayerInfoRemovePacket(List.of(handle.getUUID()));
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online instanceof CraftPlayer craftPlayer && !online.getUniqueId().equals(handle.getUUID())) {
                var add = ClientboundPlayerInfoUpdatePacket.createSinglePlayerInitializing(handle, listed && online.isListed(player()));
                craftPlayer.getHandle().connection.send(remove);
                craftPlayer.getHandle().connection.send(add);
            }
        }
    }
}
