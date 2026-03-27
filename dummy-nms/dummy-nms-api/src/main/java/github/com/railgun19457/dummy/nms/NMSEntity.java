package github.com.railgun19457.dummy.nms;

import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

public interface NMSEntity {

    @NotNull
    Entity getEntity();

    double getX();

    double getY();

    double getZ();

    float getYRot();

    float getXRot();
}
