package dev.dummy.nms.paper;

import com.destroystokyo.paper.profile.PlayerProfile;
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

/**
 * 基于 Paper 的假人句柄实现。
 *
 * <p>可见性策略：完全依赖 vanilla {@code ChunkMap} 的实体追踪，插件不主动 untrack/re-track。
 * 只有在需要调整 PlayerInfo 的 listed 标志或销毁假人时才手动发包。
 * 皮肤切换走 Paper {@link Player#setPlayerProfile(PlayerProfile)} API，由 Paper 负责原子广播。</p>
 */
public final class PaperDummyHandle implements DummyHandle {
    private static final String NO_COLLISION_TEAM = "dummy_no_collision";
    private static final String TEXTURES_PROPERTY = "textures";

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
        // 通过 Paper 的 PlayerProfile API 原子更新皮肤，由 Paper 负责广播 ClientboundPlayerInfoUpdatePacket。
        // 不再手动 untrack / re-track 实体，避免破坏 vanilla ChunkMap 已建立的追踪状态。
        Player player = player();
        PlayerProfile profile = player.getPlayerProfile();
        if (skin.hasTexture()) {
            profile.setProperty(new ProfileProperty(
                    TEXTURES_PROPERTY,
                    skin.value(),
                    skin.signature().isBlank() ? null : skin.signature()
            ));
        } else {
            profile.removeProperty(TEXTURES_PROPERTY);
        }
        player.setPlayerProfile(profile);
        // 同步更新模型部件（披风等），通过 NMS 更新 client information
        handle.updateOptionsNoEvents(PaperSkinSupport.withSkinModelParts(handle.clientInformation(), skin));
    }

    @Override
    public void refreshForViewer(Player viewer, boolean listed) {
        // 不再主动操作实体追踪。vanilla ChunkMap 会在玩家进入追踪范围时自动处理实体 spawn。
        // 仅同步 PlayerInfo 的 listed 标志，确保 showInTab 设置生效。
        updateListedForViewer(viewer, listed);
    }

    @Override
    public void updateListedForViewer(Player viewer, boolean listed) {
        if (viewer.getUniqueId().equals(handle.getUUID())) {
            return;
        }
        if (!(viewer instanceof CraftPlayer craftPlayer)) {
            return;
        }
        craftPlayer.getHandle().connection.send(
                ClientboundPlayerInfoUpdatePacket.updateListed(handle.getUUID(), listed)
        );
    }

    @Override
    public void remove(Component reason) {
        if (removed) {
            return;
        }
        removed = true;
        tickerTask.cancel();
        Player player = player();
        // PlayerList.remove/discard may remove a vehicle that still contains
        // this synthetic ServerPlayer. Detach first so the vehicle survives
        // dummy removal and can be reused during the next restore.
        org.bukkit.entity.Entity vehicle = player.getVehicle();
        if (vehicle != null) {
            vehicle.setPersistent(true);
            player.leaveVehicle();
        }
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
        var removeEntity = new ClientboundRemoveEntitiesPacket(handle.getId());
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online instanceof CraftPlayer craftPlayer && !online.getUniqueId().equals(handle.getUUID())) {
                craftPlayer.getHandle().connection.send(removeEntity);
                craftPlayer.getHandle().connection.send(removeInfo);
            }
        }
    }

    private void closeConnection() {
        if (handle.connection instanceof DummyServerGamePacketListener dummyConnection) {
            dummyConnection.closeDummyConnection();
        }
    }
}
