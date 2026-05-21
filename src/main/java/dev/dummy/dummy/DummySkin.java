package dev.dummy.dummy;

import dev.dummy.i18n.LocalizedException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.bukkit.configuration.ConfigurationSection;

public record DummySkin(String type, String value, String signature, int modelParts, long fetchedAt) {
    public static final int CAPE_PART = 0x01;
    public static final int JACKET_PART = 0x02;
    public static final int LEFT_SLEEVE_PART = 0x04;
    public static final int RIGHT_SLEEVE_PART = 0x08;
    public static final int LEFT_PANTS_PART = 0x10;
    public static final int RIGHT_PANTS_PART = 0x20;
    public static final int HAT_PART = 0x40;
    public static final int ALL_MODEL_PARTS = CAPE_PART
            | JACKET_PART
            | LEFT_SLEEVE_PART
            | RIGHT_SLEEVE_PART
            | LEFT_PANTS_PART
            | RIGHT_PANTS_PART
            | HAT_PART;
    public static final DummySkin NONE = new DummySkin("none", "", "", ALL_MODEL_PARTS & ~CAPE_PART, 0L);

    public DummySkin(String type, String value, String signature) {
        this(type, value, signature, defaultModelParts(value), 0L);
    }

    public DummySkin(String type, String value, String signature, int modelParts) {
        this(type, value, signature, modelParts, System.currentTimeMillis());
    }

    public DummySkin {
        value = value == null ? "" : value;
        signature = signature == null ? "" : signature;
        modelParts = sanitizeModelParts(modelParts, value);
    }

    public static DummySkin texture(String value, String signature) {
        return texture(value, signature, defaultModelParts(value));
    }

    public static DummySkin texture(String value, String signature, int modelParts) {
        if (value == null || value.isBlank()) {
            throw new LocalizedException("error.skin-value-blank");
        }
        return new DummySkin("texture", value, signature, modelParts);
    }

    public static DummySkin player(String playerName, String value, String signature) {
        return player(playerName, value, signature, defaultModelParts(value));
    }

    public static DummySkin player(String playerName, String value, String signature, int modelParts) {
        DummySkin texture = texture(value, signature, modelParts);
        return new DummySkin("player:" + playerName, texture.value(), texture.signature(), texture.modelParts());
    }

    public static DummySkin fromConfig(ConfigurationSection section) {
        if (section == null) {
            return NONE;
        }
        String value = section.getString("value", "");
        return new DummySkin(
                section.getString("type", "none"),
                value,
                section.getString("signature", ""),
                section.getInt("model-parts", defaultModelParts(value)),
                section.getLong("fetched-at", 0L)
        );
    }

    public boolean hasTexture() {
        return !value.isBlank();
    }

    public boolean hasCapeTexture() {
        return hasCapeTexture(value);
    }

    public boolean showsCape() {
        return hasCapeTexture() && (modelParts & CAPE_PART) != 0;
    }

    public DummySkin withFetchedAt(long fetchedAt) {
        return new DummySkin(type, value, signature, modelParts, fetchedAt);
    }

    private static int defaultModelParts(String value) {
        int parts = ALL_MODEL_PARTS & ~CAPE_PART;
        if (hasCapeTexture(value)) {
            parts |= CAPE_PART;
        }
        return parts;
    }

    private static int sanitizeModelParts(int modelParts, String value) {
        int parts = modelParts & ALL_MODEL_PARTS;
        if (!hasCapeTexture(value)) {
            parts &= ~CAPE_PART;
        }
        return parts;
    }

    private static boolean hasCapeTexture(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            String json = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
            return json.contains("\"CAPE\"");
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
