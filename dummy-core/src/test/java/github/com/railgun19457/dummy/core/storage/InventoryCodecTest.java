package github.com.railgun19457.dummy.core.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventoryCodecTest {

    @Test
    void shouldDecodeEmptyAsZeroLength() {
        assertEquals(0, InventoryCodec.decode("").length);
    }

    @Test
    void shouldThrowForMalformedInventoryPayload() {
        assertThrows(RuntimeException.class, () -> InventoryCodec.decode("not-json"));
    }
}
