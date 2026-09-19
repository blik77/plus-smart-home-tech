package ru.yandex.practicum.analyzer.service;

import ru.yandex.practicum.analyzer.model.ActionToExecute;
import ru.yandex.practicum.analyzer.model.entity.Scenario;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.util.List;

public interface ScenarioAnalyzer {
    List<ActionToExecute> analyze(SensorsSnapshotAvro snapshot, List<Scenario> scenarios);
}