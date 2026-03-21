package github.com.railgun19457.dummy.core.scheduler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActionModeTest {

    @Test
    void shouldParseContinuousMode() {
        ActionMode mode = ActionMode.parse("continuous");
        assertEquals(ActionMode.Type.CONTINUOUS, mode.type());
        assertEquals(1L, mode.intervalTicks());
    }

    @Test
    void shouldParseIntervalMode() {
        ActionMode mode = ActionMode.parse("interval:20");
        assertEquals(ActionMode.Type.INTERVAL, mode.type());
        assertEquals(20L, mode.intervalTicks());
    }

    @Test
    void shouldThrowForInvalidMode() {
        assertThrows(IllegalArgumentException.class, () -> ActionMode.parse("bad-mode"));
    }
}
