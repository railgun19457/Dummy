package io.github.railgun19457.dummy.core.model;

import java.util.List;

public record DummyInventoryState(
        List<String> contents,
        List<String> armorContents,
        String offhandItem
) {
    public static DummyInventoryState empty() {
        return new DummyInventoryState(List.of(), List.of(), null);
    }
}
