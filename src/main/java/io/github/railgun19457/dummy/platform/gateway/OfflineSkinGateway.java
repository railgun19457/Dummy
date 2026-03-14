package io.github.railgun19457.dummy.platform.gateway;

import io.github.railgun19457.dummy.core.model.SkinProfile;
import io.github.railgun19457.dummy.core.port.SkinGateway;

import java.util.concurrent.CompletableFuture;

public final class OfflineSkinGateway implements SkinGateway {

    @Override
    public CompletableFuture<SkinProfile> fetchByPlayerName(String playerName) {
        return CompletableFuture.completedFuture(new SkinProfile(playerName, null, null));
    }
}
