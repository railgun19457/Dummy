package io.github.railgun19457.dummy.core.service;

import io.github.railgun19457.dummy.core.model.DummyDefinition;
import io.github.railgun19457.dummy.core.model.DummyId;
import io.github.railgun19457.dummy.core.port.DummyRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class DummyQueryService {

    private final DummyRepository dummyRepository;

    public DummyQueryService(DummyRepository dummyRepository) {
        this.dummyRepository = dummyRepository;
    }

    public Optional<DummyDefinition> findById(DummyId dummyId) {
        return dummyRepository.findById(dummyId);
    }

    public List<DummyDefinition> findByOwner(UUID ownerId) {
        return dummyRepository.findByOwner(ownerId);
    }

    public List<DummyDefinition> findAll() {
        return dummyRepository.findAll();
    }
}
