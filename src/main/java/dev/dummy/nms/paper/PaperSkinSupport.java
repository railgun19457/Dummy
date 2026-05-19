package dev.dummy.nms.paper;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.google.common.collect.HashMultimap;
import dev.dummy.dummy.DummySkin;
import java.util.UUID;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.world.entity.player.PlayerModelPart;

final class PaperSkinSupport {
    private static final int ALL_MODEL_PARTS = allModelParts();

    private PaperSkinSupport() {
    }

    static GameProfile createProfile(UUID uuid, String name, DummySkin skin) {
        var properties = HashMultimap.<String, Property>create();
        if (skin.hasTexture()) {
            Property property = skin.signature().isBlank()
                    ? new Property("textures", skin.value())
                    : new Property("textures", skin.value(), skin.signature());
            properties.put("textures", property);
        }
        return new GameProfile(uuid, name, new PropertyMap(properties));
    }

    static ClientInformation withAllModelParts(ClientInformation information) {
        return new ClientInformation(
                information.language(),
                information.viewDistance(),
                information.chatVisibility(),
                information.chatColors(),
                ALL_MODEL_PARTS,
                information.mainHand(),
                information.textFilteringEnabled(),
                true,
                information.particleStatus()
        );
    }

    private static int allModelParts() {
        int mask = 0;
        for (PlayerModelPart part : PlayerModelPart.values()) {
            mask |= part.getMask();
        }
        return mask;
    }
}
