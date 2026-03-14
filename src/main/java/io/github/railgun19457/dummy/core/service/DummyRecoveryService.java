package io.github.railgun19457.dummy.core.service;

import io.github.railgun19457.dummy.common.config.PluginConfig;
import io.github.railgun19457.dummy.core.model.DummyDefinition;
import io.github.railgun19457.dummy.core.model.DummyId;
import io.github.railgun19457.dummy.core.model.DummyRuntimeState;
import io.github.railgun19457.dummy.core.port.CompatBridge;
import io.github.railgun19457.dummy.core.port.DummyRepository;

import java.util.List;
import java.util.Map;

public final class DummyRecoveryService {

    private final PluginConfig pluginConfig;
    private final DummyRepository dummyRepository;
    private final DummyLifecycleService lifecycleService;

    public DummyRecoveryService(
            PluginConfig pluginConfig,
            DummyRepository dummyRepository,
            CompatBridge compatBridge,
            DummyLifecycleService lifecycleService
    ) {
        this.pluginConfig = pluginConfig;
        this.dummyRepository = dummyRepository;
        this.lifecycleService = lifecycleService;
    }

    public List<DummyDefinition> recoverAll() {
        if (!pluginConfig.persistence().saveDummyData()) {
            return List.of();
        }
        List<DummyDefinition> definitions = dummyRepository.findAll();
        Map<DummyId, DummyRuntimeState> runtimeSnapshots = dummyRepository.findAllRuntimeStates();
        definitions.forEach(definition -> {
            lifecycleService.registerRecoveredDummy(definition, runtimeSnapshots.getOrDefault(
                    definition.id(),
                    new DummyRuntimeState(definition.id(), true, null, null, false, false, false, null)
            ));
        });
        return definitions;
    }
}
