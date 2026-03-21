package github.com.railgun19457.dummy.core.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import github.com.railgun19457.dummy.core.model.DummyPosition;
import github.com.railgun19457.dummy.core.model.DummySession;
import github.com.railgun19457.dummy.core.model.DummyTraits;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SessionSerializationTest {

    @Test
    void shouldSerializeAndDeserializeSession() {
        DummySession source = new DummySession(
                UUID.randomUUID(),
                "bot1",
                UUID.randomUUID(),
                "owner",
                "skinOwner",
                new DummyTraits(true, false, true, false),
                new DummyPosition("world", 1, 64, 1, 0, 0),
                "inventory-json",
                Instant.now()
        );

        Gson gson = new GsonBuilder()
            .registerTypeAdapter(Instant.class, (com.google.gson.JsonSerializer<Instant>) (src, typeOfSrc, context) -> new com.google.gson.JsonPrimitive(src.toString()))
            .registerTypeAdapter(Instant.class, (com.google.gson.JsonDeserializer<Instant>) (json, typeOfT, context) -> Instant.parse(json.getAsString()))
            .create();
        String json = gson.toJson(source);
        DummySession restored = gson.fromJson(json, DummySession.class);

        assertEquals(source.name(), restored.name());
        assertEquals(source.skinSource(), restored.skinSource());
        assertEquals(source.position().world(), restored.position().world());
        assertEquals(source.inventoryData(), restored.inventoryData());
    }
}
