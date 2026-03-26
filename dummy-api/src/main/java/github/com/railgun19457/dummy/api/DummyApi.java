package github.com.railgun19457.dummy.api;

import github.com.railgun19457.dummy.api.model.DummySession;

import java.util.List;
import java.util.Optional;

public interface DummyApi {

    Optional<DummySession> findByName(String name);

    List<DummySession> listAll();
}
