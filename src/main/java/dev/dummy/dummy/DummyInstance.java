package dev.dummy.dummy;

import dev.dummy.nms.DummyHandle;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class DummyInstance {
    private final UUID uuid;
    private final UUID creatorUuid;
    private final String creatorName;
    private final String name;
    private final DummyHandle handle;
    private DummySettings settings;
    private DummySkin skin;

    public DummyInstance(UUID uuid, UUID creatorUuid, String creatorName, String name, DummySettings settings, DummySkin skin, DummyHandle handle) {
        this.uuid = uuid;
        this.creatorUuid = creatorUuid;
        this.creatorName = creatorName;
        this.name = name;
        this.settings = settings;
        this.skin = skin;
        this.handle = handle;
    }

    public UUID uuid() {
        return uuid;
    }

    public UUID creatorUuid() {
        return creatorUuid;
    }

    public String creatorName() {
        return creatorName;
    }

    public String name() {
        return name;
    }

    public DummySettings settings() {
        return settings;
    }

    public void settings(DummySettings settings) {
        this.settings = settings;
    }

    public DummySkin skin() {
        return skin;
    }

    public void skin(DummySkin skin) {
        this.skin = skin;
    }

    public DummyHandle handle() {
        return handle;
    }

    public Player player() {
        return handle.player();
    }

    public Location location() {
        return player().getLocation();
    }

    public DummyExperience experience() {
        return DummyExperience.fromPlayer(player());
    }

    public void applyRecord(DummyRecord record) {
        Player player = player();
        PlayerInventory inventory = player.getInventory();
        inventory.setStorageContents(copy(record.storageContents(), inventory.getStorageContents().length));
        inventory.setArmorContents(copy(record.armorContents(), inventory.getArmorContents().length));
        inventory.setItemInOffHand(record.offhandItem());
        record.experience().apply(player);
    }

    private ItemStack[] copy(ItemStack[] source, int size) {
        ItemStack[] result = new ItemStack[size];
        if (source == null) {
            return result;
        }
        System.arraycopy(source, 0, result, 0, Math.min(source.length, result.length));
        return result;
    }
}
