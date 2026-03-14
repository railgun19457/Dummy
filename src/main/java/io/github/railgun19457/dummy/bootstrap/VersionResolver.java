package io.github.railgun19457.dummy.bootstrap;

public final class VersionResolver {

    private VersionResolver() {
    }

    public static String resolveCompatVersion(String minecraftVersion) {
        if (minecraftVersion == null || minecraftVersion.isBlank()) {
            throw new IllegalArgumentException("minecraftVersion must not be blank");
        }
        if (minecraftVersion.startsWith("1.20")) {
            return "v120x";
        }
        if (minecraftVersion.startsWith("1.21")) {
            return "v121x";
        }
        throw new IllegalStateException("Unsupported Minecraft version: " + minecraftVersion);
    }
}
