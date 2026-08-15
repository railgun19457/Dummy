package dev.dummy.dummy;

import java.util.List;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

public record DummyRecord(
        UUID uuid,
        UUID creatorUuid,
        String creatorName,
        String name,
        Location location,
        DummySettings settings,
        DummySkin skin,
        ItemStack[] storageContents,
        ItemStack[] armorContents,
        ItemStack offhandItem,
        DummyExperience experience,
        UUID vehicleUuid,
        List<String> activeActions
) {
    /**
     * Backwards-compatible constructor for callers that do not yet carry
     * vehicle/action state.
     */
    public DummyRecord(
            UUID uuid,
            UUID creatorUuid,
            String creatorName,
            String name,
            Location location,
            DummySettings settings,
            DummySkin skin,
            ItemStack[] storageContents,
            ItemStack[] armorContents,
            ItemStack offhandItem,
            DummyExperience experience
    ) {
        this(uuid, creatorUuid, creatorName, name, location, settings, skin, storageContents, armorContents, offhandItem, experience, null, List.of());
    }
}
