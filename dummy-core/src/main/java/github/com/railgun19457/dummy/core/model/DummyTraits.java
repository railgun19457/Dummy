package github.com.railgun19457.dummy.core.model;

public record DummyTraits(
        boolean collision,
        boolean invulnerable,
        boolean autoRestock,
        boolean autoFishing
) {

    public DummyTraits toggleCollision() {
        return new DummyTraits(!collision, invulnerable, autoRestock, autoFishing);
    }

    public DummyTraits toggleInvulnerable() {
        return new DummyTraits(collision, !invulnerable, autoRestock, autoFishing);
    }

    public DummyTraits toggleAutoRestock() {
        return new DummyTraits(collision, invulnerable, !autoRestock, autoFishing);
    }

    public DummyTraits toggleAutoFishing() {
        return new DummyTraits(collision, invulnerable, autoRestock, !autoFishing);
    }
}
