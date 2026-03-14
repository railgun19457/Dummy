package io.github.railgun19457.dummy.common.config;

public record PluginConfig(
        String defaultLocale,
        LimitConfig limits,
        NamingConfig naming,
        SleepConfig sleep,
        RemovalConfig removal,
        PersistenceConfig persistence,
        TraitDefaultConfig traitDefaults
) {
}
