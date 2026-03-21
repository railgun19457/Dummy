package github.com.railgun19457.dummy.nms;

import github.com.railgun19457.dummy.core.model.DummySession;

public interface NmsBridge {

	String spawnDummy(DummySession session);

	void removeDummy(DummySession session);
}
