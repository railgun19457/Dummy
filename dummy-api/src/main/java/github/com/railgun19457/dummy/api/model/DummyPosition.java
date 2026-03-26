package github.com.railgun19457.dummy.api.model;

public record DummyPosition(
        String world,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
) {
}
