package github.com.railgun19457.dummy.core.scheduler;

public record ActionMode(Type type, long intervalTicks) {

    public enum Type {
        ONCE,
        CONTINUOUS,
        INTERVAL
    }

    public static ActionMode once() {
        return new ActionMode(Type.ONCE, 0);
    }

    public static ActionMode continuous() {
        return new ActionMode(Type.CONTINUOUS, 1);
    }

    public static ActionMode interval(long ticks) {
        return new ActionMode(Type.INTERVAL, Math.max(1, ticks));
    }

    public static ActionMode parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return once();
        }

        String value = raw.toLowerCase();
        if ("once".equals(value)) {
            return once();
        }
        if ("continuous".equals(value)) {
            return continuous();
        }
        if (value.startsWith("interval:")) {
            long ticks = Long.parseLong(value.substring("interval:".length()));
            return interval(ticks);
        }
        throw new IllegalArgumentException("invalid mode: " + raw);
    }
}
