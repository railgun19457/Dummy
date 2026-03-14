package io.github.railgun19457.dummy.bootstrap;

import io.github.railgun19457.dummy.common.config.PluginConfig;
import io.github.railgun19457.dummy.i18n.MessageBundleRegistry;
import org.bukkit.plugin.java.JavaPlugin;

public record BootstrapContext(
        JavaPlugin plugin,
        String platform,
        String minecraftVersion,
        boolean paperServer,
        PluginConfig pluginConfig,
        MessageBundleRegistry messageBundleRegistry
) {
}
