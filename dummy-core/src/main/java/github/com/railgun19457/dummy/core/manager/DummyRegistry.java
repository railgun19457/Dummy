package github.com.railgun19457.dummy.core.manager;

import github.com.railgun19457.dummy.api.model.DummySession;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class DummyRegistry {

    private final Map<String, DummySession> byName = new ConcurrentHashMap<>();

    public int size() {
        return byName.size();
    }

    public boolean exists(String name) {
        return byName.containsKey(normalize(name));
    }

    public Optional<DummySession> find(String name) {
        return Optional.ofNullable(byName.get(normalize(name)));
    }

    public void put(DummySession session) {
        byName.put(normalize(session.name()), session);
    }

    public Optional<DummySession> remove(String name) {
        return Optional.ofNullable(byName.remove(normalize(name)));
    }

    public List<DummySession> listAll() {
        return byName.values().stream()
                .sorted(Comparator.comparing(DummySession::createdAt))
                .toList();
    }

    public List<DummySession> listByOwner(java.util.UUID ownerUuid) {
        List<DummySession> result = new ArrayList<>();
        for (DummySession value : byName.values()) {
            if (value.ownerUuid().equals(ownerUuid)) {
                result.add(value);
            }
        }
        result.sort(Comparator.comparing(DummySession::createdAt));
        return result;
    }

    private String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
