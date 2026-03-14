package io.github.railgun19457.dummy.core.port;

import io.github.railgun19457.dummy.core.model.SkinProfile;

import java.util.concurrent.CompletableFuture;

public interface SkinGateway {

    CompletableFuture<SkinProfile> fetchByPlayerName(String playerName);
}
