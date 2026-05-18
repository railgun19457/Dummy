package dev.dummy.nms;

import dev.dummy.dummy.DummySettings;
import dev.dummy.dummy.DummySkin;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface DummyHandle {
    Player player();

    void teleport(Location location);

    void applySettings(String name, DummySettings settings);

    void applySkin(DummySkin skin);

    void remove(Component reason);
}
