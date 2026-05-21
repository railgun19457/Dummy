package dev.dummy.nms.paper;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.google.common.collect.HashMultimap;
import dev.dummy.dummy.DummySkin;
import java.util.UUID;
import net.minecraft.server.level.ClientInformation;

final class PaperSkinSupport {
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

    static ClientInformation withSkinModelParts(ClientInformation information, DummySkin skin) {
        return new ClientInformation(
                information.language(),
                information.viewDistance(),
                information.chatVisibility(),
                information.chatColors(),
                skin.modelParts(),
                information.mainHand(),
                information.textFilteringEnabled(),
                true,
                information.particleStatus()
        );
    }
}
