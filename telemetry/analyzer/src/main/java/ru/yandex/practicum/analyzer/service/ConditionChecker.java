package ru.yandex.practicum.analyzer.service;

import ru.yandex.practicum.analyzer.model.entity.Condition;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;

public interface ConditionChecker {
    boolean isMet(Condition condition, SensorStateAvro state);
}