package ru.yandex.practicum.analyzer.service;

import ru.yandex.practicum.kafka.telemetry.event.DeviceAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioAddedEventAvro;

public interface HubEventService {
    void addDevice(String hubId, DeviceAddedEventAvro event);
    void removeDevice(String hubId, String deviceId);
    void addScenario(String hubId, ScenarioAddedEventAvro event);
    void removeScenario(String hubId, String name);
}