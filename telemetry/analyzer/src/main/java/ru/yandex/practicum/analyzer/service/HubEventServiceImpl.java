package ru.yandex.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.analyzer.model.ActionType;
import ru.yandex.practicum.analyzer.model.ConditionOperation;
import ru.yandex.practicum.analyzer.model.ConditionType;
import ru.yandex.practicum.analyzer.model.entity.*;
import ru.yandex.practicum.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.analyzer.repository.SensorRepository;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HubEventServiceImpl implements HubEventService {

    private final SensorRepository sensorRepository;
    private final ScenarioRepository scenarioRepository;

    @Override
    @Transactional
    public void addDevice(String hubId, DeviceAddedEventAvro event) {
        String deviceId = event.getId().toString();
        sensorRepository.findByIdAndHubId(deviceId, hubId).ifPresentOrElse(
            sensor -> log.debug("Устройство {} уже есть в хабе {}", deviceId, hubId),
            () -> {
                sensorRepository.save(Sensor.builder().id(deviceId).hubId(hubId).build());
                log.info("Добавлено устройство {} в хаб {}", deviceId, hubId);
            });
    }

    @Override
    @Transactional
    public void removeDevice(String hubId, String deviceId) {
        List<Scenario> scenarios = scenarioRepository.findByHubId(hubId);
        for (Scenario scenario : scenarios) {
            scenario.getConditions().removeIf(sc -> sc.getSensor().getId().equals(deviceId));
            scenario.getActions().removeIf(sa -> sa.getSensor().getId().equals(deviceId));
        }
        sensorRepository.findByIdAndHubId(deviceId, hubId).ifPresent(sensor -> {
            sensorRepository.delete(sensor);
            log.info("Удалено устройство {} из хаба {}", deviceId, hubId);
        });
    }

    @Override
    @Transactional
    public void addScenario(String hubId, ScenarioAddedEventAvro event) {
        String name = event.getName().toString();
        Scenario scenario = scenarioRepository.findByHubIdAndName(hubId, name)
            .orElseGet(() -> Scenario.builder().hubId(hubId).name(name).build());

        scenario.getConditions().clear();
        scenario.getActions().clear();

        Set<String> sensorIds = new HashSet<>();
        event.getConditions().forEach(c -> sensorIds.add(c.getSensorId().toString()));
        event.getActions().forEach(a -> sensorIds.add(a.getSensorId().toString()));

        Map<String, Sensor> sensors = sensorRepository.findAllByIdInAndHubId(sensorIds, hubId)
            .stream()
            .collect(Collectors.toMap(Sensor::getId, Function.identity()));

        for (ScenarioConditionAvro c : event.getConditions()) {
            Sensor sensor = sensors.get(c.getSensorId().toString());
            if (sensor != null) {
                scenario.getConditions().add(ScenarioCondition.builder()
                    .scenario(scenario)
                    .sensor(sensor)
                    .condition(Condition.builder()
                        .type(ConditionType.valueOf(c.getType().name()))
                        .operation(ConditionOperation.valueOf(c.getOperation().name()))
                        .value(toInt(c.getValue()))
                        .build())
                    .build());
            } else {
                log.warn("Датчик {} не найден в хабе {}, условие пропущено", c.getSensorId(), hubId);
            }
        }

        for (DeviceActionAvro a : event.getActions()) {
            Sensor sensor = sensors.get(a.getSensorId().toString());
            if (sensor != null) {
                scenario.getActions().add(ScenarioAction.builder()
                    .scenario(scenario)
                    .sensor(sensor)
                    .action(Action.builder()
                        .type(ActionType.valueOf(a.getType().name()))
                        .value(a.getValue())
                        .build())
                    .build());
            } else {
                log.warn("Датчик {} не найден в хабе {}, действие пропущено", a.getSensorId(), hubId);
            }
        }

        scenarioRepository.save(scenario);
        log.info("Сохранён сценарий '{}' хаба {}: условий={}, действий={}",
            name, hubId, scenario.getConditions().size(), scenario.getActions().size());
    }

    @Override
    @Transactional
    public void removeScenario(String hubId, String name) {
        scenarioRepository.findByHubIdAndName(hubId, name).ifPresent(scenario -> {
            scenarioRepository.delete(scenario);
            log.info("Удалён сценарий '{}' хаба {}", name, hubId);
        });
    }

    private Integer toInt(Object value) {
        if (value instanceof Integer i) {
            return i;
        }
        if (value instanceof Boolean b) {
            return b ? 1 : 0;
        }
        return null;
    }
}