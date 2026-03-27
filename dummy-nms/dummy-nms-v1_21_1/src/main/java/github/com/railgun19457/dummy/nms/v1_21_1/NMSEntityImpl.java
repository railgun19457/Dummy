package github.com.railgun19457.dummy.nms.v1_21_1;

import github.com.railgun19457.dummy.nms.NMSEntity;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

public class NMSEntityImpl implements NMSEntity {

    private final net.minecraft.world.entity.Entity handle;
    private final Entity entity;

    public NMSEntityImpl(@NotNull Entity entity) {
        this.entity = entity;
        this.handle = ((CraftEntity) entity).getHandle();
    }

    @Override
    public @NotNull Entity getEntity() {
        return entity;
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
    public float getYRot() {
        return handle.getYRot();
    }

    @Override
    public float getXRot() {
        return handle.getXRot();
    }
}
