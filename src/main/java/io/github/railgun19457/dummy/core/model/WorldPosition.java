package io.github.railgun19457.dummy.core.model;

public record WorldPosition(
        String worldName,
        double x,
        double y,
        double z,
        Rotation rotation
) {
}
