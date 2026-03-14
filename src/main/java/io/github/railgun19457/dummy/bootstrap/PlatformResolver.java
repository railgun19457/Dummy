package io.github.railgun19457.dummy.bootstrap;

import org.bukkit.Bukkit;

public final class PlatformResolver {

    private PlatformResolver() {
    }

    public static String resolvePlatformName() {
        return Bukkit.getName();
    }

    public static boolean isPaperServer() {
        try {
            Class.forName("com.destroystokyo.paper.ParticleBuilder");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}
