package dev.dummy.nms.paper;

import dev.dummy.DummyPlugin;
import dev.dummy.dummy.DummySettings;
import dev.dummy.dummy.DummySkin;
import dev.dummy.nms.DummyHandle;
import dev.dummy.nms.paper.compat.PaperNmsCompatibility;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    private static final long PROFILE_TO_ENTITY_DELAY_TICKS = 10L;
    private static final long PROFILE_REPLACE_DELAY_TICKS = 20L;
    private static final long HIDE_TAB_DELAY_TICKS = 40L;

    private final DummyPlugin plugin;
    private final ServerPlayer handle;
    private final PaperNmsCompatibility nmsCompatibility;
    private final BukkitTask tickerTask;
    private final Map<UUID, Long> viewerEntityGenerations = new LinkedHashMap<>();
    private final Map<UUID, Long> viewerTabGenerations = new LinkedHashMap<>();
    private boolean removed;
    private boolean listed = true;
    private long entityRefreshGeneration;
    private long tabRefreshGeneration;

    public PaperDummyHandle(DummyPlugin plugin, ServerPlayer handle, PaperNmsCompatibility nmsCompatibility, BukkitTask tickerTask) {
        this.plugin = plugin;
        this.handle = handle;
        this.nmsCompatibility = nmsCompatibility;
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
        handle.updateOptionsNoEvents(PaperSkinSupport.withSkinModelParts(handle.clientInformation(), skin));
        refreshSkinForViewers();
    }

    @Override
    public void refreshForViewer(Player viewer, boolean listed) {
        refreshProfileForViewer(viewer, listed, PROFILE_TO_ENTITY_DELAY_TICKS);
    }

    @Override
    public void updateListedForViewer(Player viewer, boolean listed) {
        if (viewer.getUniqueId().equals(handle.getUUID())) {
            return;
        }
        nextTabGeneration(viewer);
        if (listed) {
            sendPlayerInfo(viewer, true);
            return;
        }
        sendListed(viewer, false);
    }

    private void refreshProfileForViewer(Player viewer, boolean listed) {
        refreshProfileForViewer(viewer, listed, PROFILE_REPLACE_DELAY_TICKS);
    }

    private void refreshProfileForViewer(Player viewer, boolean listed, long respawnDelayTicks) {
        if (viewer.getUniqueId().equals(handle.getUUID())) {
            return;
        }
        long entityGeneration = nextEntityGeneration(viewer);
        long tabGeneration = nextTabGeneration(viewer);
        untrackForViewer(viewer);
        sendPlayerInfoRemove(viewer);
        sendPlayerInfo(viewer, true);
        runIfCurrentEntity(viewer, entityGeneration, respawnDelayTicks, () -> trackForViewer(viewer));
        if (!listed) {
            runIfCurrentTab(viewer, tabGeneration, respawnDelayTicks + HIDE_TAB_DELAY_TICKS, () -> sendListed(viewer, false));
        }
    }

    @Override
    public void remove(Component reason) {
        if (removed) {
            return;
        }
        removed = true;
        tickerTask.cancel();
        viewerEntityGenerations.clear();
        viewerTabGenerations.clear();
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

    private void sendRemoveEntityPacket(Player viewer) {
        if (viewer instanceof CraftPlayer craftPlayer) {
            craftPlayer.getHandle().connection.send(new ClientboundRemoveEntitiesPacket(handle.getId()));
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
        for (Player online : Bukkit.getOnlinePlayers()) {
            refreshProfileForViewer(online, listed);
        }
    }

    private void sendPlayerInfoRemove(Player viewer) {
        if (viewer instanceof CraftPlayer craftPlayer) {
            craftPlayer.getHandle().connection.send(new ClientboundPlayerInfoRemovePacket(List.of(handle.getUUID())));
        }
    }

    private void sendPlayerInfo(Player viewer, boolean listed) {
        if (viewer instanceof CraftPlayer craftPlayer) {
            var add = ClientboundPlayerInfoUpdatePacket.createSinglePlayerInitializing(handle, listed);
            craftPlayer.getHandle().connection.send(add);
        }
    }

    private void sendListed(Player viewer, boolean listed) {
        if (viewer instanceof CraftPlayer craftPlayer) {
            craftPlayer.getHandle().connection.send(ClientboundPlayerInfoUpdatePacket.updateListed(handle.getUUID(), listed));
        }
    }

    private void untrackForViewer(Player viewer) {
        if (!(viewer instanceof CraftPlayer craftPlayer) || !nmsCompatibility.removeTrackedViewer(handle, craftPlayer.getHandle())) {
            sendRemoveEntityPacket(viewer);
        }
    }

    private void trackForViewer(Player viewer) {
        if (!(viewer instanceof CraftPlayer craftPlayer) || !nmsCompatibility.updateTrackedViewer(handle, craftPlayer.getHandle())) {
            sendEntityPairingData(viewer);
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

    private long nextEntityGeneration(Player viewer) {
        long generation = ++entityRefreshGeneration;
        viewerEntityGenerations.put(viewer.getUniqueId(), generation);
        return generation;
    }

    private long nextTabGeneration(Player viewer) {
        long generation = ++tabRefreshGeneration;
        viewerTabGenerations.put(viewer.getUniqueId(), generation);
        return generation;
    }

    private void runIfCurrentEntity(Player viewer, long generation, long delayTicks, Runnable task) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!removed && player().isOnline() && viewer.isOnline() && isCurrent(viewerEntityGenerations, viewer, generation)) {
                task.run();
            }
        }, delayTicks);
    }

    private void runIfCurrentTab(Player viewer, long generation, long delayTicks, Runnable task) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!removed && player().isOnline() && viewer.isOnline() && isCurrent(viewerTabGenerations, viewer, generation)) {
                task.run();
            }
        }, delayTicks);
    }

    private boolean isCurrent(Map<UUID, Long> generations, Player viewer, long generation) {
        Long current = generations.get(viewer.getUniqueId());
        return current != null && current == generation;
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
