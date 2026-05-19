package dev.dummy.nms.paper;

import dev.dummy.DummyPlugin;
import dev.dummy.dummy.DummySettings;
import dev.dummy.dummy.DummySkin;
import dev.dummy.nms.DummyHandle;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.level.ServerEntity;
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

    private final DummyPlugin plugin;
    private final ServerPlayer handle;
    private final BukkitTask tickerTask;
    private boolean removed;
    private boolean listed = true;

    public PaperDummyHandle(DummyPlugin plugin, ServerPlayer handle, BukkitTask tickerTask) {
        this.plugin = plugin;
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
        handle.gameProfile = PaperSkinSupport.createProfile(handle.getUUID(), handle.getGameProfile().name(), skin);
        handle.updateOptionsNoEvents(PaperSkinSupport.withAllModelParts(handle.clientInformation()));
        refreshSkinForViewers();
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

    private void refreshSkinForViewers() {
        sendRemovePackets();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player().isOnline()) {
                return;
            }
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getUniqueId().equals(handle.getUUID())) {
                    continue;
                }
                sendPlayerInfo(online, true);
            }
        }, 5L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player().isOnline()) {
                return;
            }
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getUniqueId().equals(handle.getUUID())) {
                    continue;
                }
                sendEntityPairingData(online);
            }
        }, 10L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (listed || !player().isOnline()) {
                return;
            }
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getUniqueId().equals(handle.getUUID()) || !(online instanceof CraftPlayer craftPlayer)) {
                    continue;
                }
                craftPlayer.getHandle().connection.send(ClientboundPlayerInfoUpdatePacket.updateListed(handle.getUUID(), false));
            }
        }, 50L);
    }

    private void sendPlayerInfo(Player viewer, boolean listed) {
        if (viewer instanceof CraftPlayer craftPlayer) {
            var add = ClientboundPlayerInfoUpdatePacket.createSinglePlayerInitializing(handle, listed);
            craftPlayer.getHandle().connection.send(add);
        }
    }

    private void sendEntityPairingData(Player viewer) {
        if (!(viewer instanceof CraftPlayer craftPlayer)) {
            return;
        }
        ServerEntity entityTracker = new ServerEntity(handle.level(), handle, 0, false, NoOpSynchronizer.INSTANCE, java.util.Set.of());
        java.util.List<Packet<? super ClientGamePacketListener>> packets = new java.util.ArrayList<>();
        entityTracker.sendPairingData(craftPlayer.getHandle(), packets::add);
        if (!packets.isEmpty()) {
            craftPlayer.getHandle().connection.send(new ClientboundBundlePacket(packets));
        }
    }

    private enum NoOpSynchronizer implements ServerEntity.Synchronizer {
        INSTANCE;

        @Override
        public void sendToTrackingPlayers(Packet<? super ClientGamePacketListener> packet) {
        }

        @Override
        public void sendToTrackingPlayersAndSelf(Packet<? super ClientGamePacketListener> packet) {
        }

        @Override
        public void sendToTrackingPlayersFiltered(Packet<? super ClientGamePacketListener> packet, java.util.function.Predicate<ServerPlayer> predicate) {
        }
    }

}
