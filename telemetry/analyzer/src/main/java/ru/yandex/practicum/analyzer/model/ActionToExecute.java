package ru.yandex.practicum.analyzer.model;

import java.time.Instant;

public record ActionToExecute(
    String hubId,
    String scenarioName,
    String sensorId,
    ActionType type,
    Integer value,
    Instant timestamp
) {
}