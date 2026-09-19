package ru.yandex.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.analyzer.model.ActionToExecute;
import ru.yandex.practicum.analyzer.model.entity.Scenario;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioAnalyzerImpl implements ScenarioAnalyzer {

    private final ConditionChecker conditionChecker;

    @Override
    public List<ActionToExecute> analyze(SensorsSnapshotAvro snapshot, List<Scenario> scenarios) {
        Map<String, SensorStateAvro> states = snapshot.getSensorsState();

        return scenarios.stream()
            .filter(scenario -> allConditionsMet(scenario, states))
            .peek(scenario -> log.info("Сценарий '{}' хаба {} сработал",
                scenario.getName(), scenario.getHubId()))
            .flatMap(scenario -> scenario.getActions().stream()
                .map(sa -> toActionToExecute(snapshot, scenario, sa)))
            .toList();
    }

    private boolean allConditionsMet(
        Scenario scenario,
        Map<String, SensorStateAvro> states
    ) {
        return scenario.getConditions().stream()
            .allMatch(sc -> conditionChecker.isMet(
                sc.getCondition(),
                states.get(sc.getSensor().getId())));
    }

    private ActionToExecute toActionToExecute(
        SensorsSnapshotAvro snapshot,
        Scenario scenario,
        ru.yandex.practicum.analyzer.model.entity.ScenarioAction sa
    ) {
        return new ActionToExecute(
            snapshot.getHubId().toString(),
            scenario.getName(),
            sa.getSensor().getId(),
            sa.getAction().getType(),
            sa.getAction().getValue(),
            snapshot.getTimestamp());
    }
}