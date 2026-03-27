package github.com.railgun19457.dummy.nms.v1_21_1;

import github.com.railgun19457.dummy.nms.NMSServerPlayer;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class NMSServerPlayerImpl implements NMSServerPlayer {

    private final ServerPlayer handle;
    private final CraftPlayer player;

    public NMSServerPlayerImpl(@NotNull Player player) {
        this.player = (CraftPlayer) player;
        this.handle = this.player.getHandle();
    }

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }

    @Override
    public double getX() {
        return handle.getX();
    }

    @Override
    public double getY() {
        return handle.getY();
    }

    @Override
    public double getZ() {
        return handle.getZ();
    }

    @Override
    public void setXo(double xo) {
        handle.xo = xo;
    }

    @Override
    public void setYo(double yo) {
        handle.yo = yo;
    }

    @Override
    public void setZo(double zo) {
        handle.zo = zo;
    }

    @Override
    public void doTick() {
        handle.doTick();
    }

    @Override
    public void absMoveTo(double x, double y, double z, float yRot, float xRot) {
        handle.absMoveTo(x, y, z, yRot, xRot);
    }

    @Override
    public float getYRot() {
        return handle.getYRot();
    }

    @Override
    public void setYRot(float yRot) {
        handle.setYRot(yRot);
    }

    @Override
    public float getXRot() {
        return handle.getXRot();
    }

    @Override
    public void setXRot(float xRot) {
        handle.setXRot(xRot);
    }

    @Override
    public float getZza() {
        return handle.zza;
    }

    @Override
    public void setZza(float zza) {
        handle.zza = zza;
    }

    @Override
    public float getXxa() {
        return handle.xxa;
    }

    @Override
    public void setXxa(float xxa) {
        handle.xxa = xxa;
    }

    @Override
    public void setDeltaMovement(@NotNull Vector vector) {
        handle.setDeltaMovement(vector.getX(), vector.getY(), vector.getZ());
    }

    @Override
    public boolean startRiding(@NotNull Entity entity, boolean force) {
        return handle.startRiding(((org.bukkit.craftbukkit.entity.CraftEntity) entity).getHandle(), force);
    }

    @Override
    public void stopRiding() {
        handle.stopRiding();
    }

    @Override
    public int getTickCount() {
        return handle.tickCount;
    }

    @Override
    public void resetLastActionTime() {
        handle.resetLastActionTime();
    }

    @Override
    public boolean onGround() {
        return handle.onGround();
    }

    @Override
    public void jumpFromGround() {
        handle.jumpFromGround();
    }

    @Override
    public void setJumping(boolean jumping) {
        handle.setJumping(jumping);
    }

    @Override
    public boolean isUsingItem() {
        return handle.isUsingItem();
    }

    @Override
    public void respawn() {
        handle.respawn();
    }

    @Override
    public void swing(boolean offhand) {
        handle.swing(offhand ? net.minecraft.world.InteractionHand.OFF_HAND : net.minecraft.world.InteractionHand.MAIN_HAND);
    }

    @Override
    public void resetAttackStrengthTicker() {
        handle.resetAttackStrengthTicker();
    }

    @Override
    public void setupClientOptions() {
        // Client options are already set during player creation
    }

    @Override
    public void disableAdvancements() {
        // Advancements disabled via DummyPlayerAdvancements
    }
}
