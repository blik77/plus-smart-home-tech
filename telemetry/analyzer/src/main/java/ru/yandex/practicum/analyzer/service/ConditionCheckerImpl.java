package ru.yandex.practicum.analyzer.service;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.analyzer.model.ConditionOperation;
import ru.yandex.practicum.analyzer.model.ConditionType;
import ru.yandex.practicum.analyzer.model.entity.Condition;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.util.Optional;

@Service
public class ConditionCheckerImpl implements ConditionChecker {

    @Override
    public boolean isMet(Condition condition, SensorStateAvro state) {
        if (state == null || condition.getValue() == null) {
            return false;
        }
        return extractValue(condition.getType(), state.getData())
            .map(actual -> compare(condition.getOperation(), actual, condition.getValue()))
            .orElse(false);
    }

    private Optional<Integer> extractValue(ConditionType type, Object data) {
        return switch (type) {
            case TEMPERATURE -> {
                if (data instanceof TemperatureSensorAvro t) {
                    yield Optional.of(t.getTemperatureC());
                } else if (data instanceof ClimateSensorAvro c) {
                    yield Optional.of(c.getTemperatureC());  // ← климат-сенсор тоже меряет температуру!
                }
                yield Optional.empty();
            }

            case LUMINOSITY -> data instanceof LightSensorAvro l
                ? Optional.of(l.getLuminosity()) : Optional.empty();
            case MOTION -> data instanceof MotionSensorAvro m
                ? Optional.of(m.getMotion() ? 1 : 0) : Optional.empty();
            case SWITCH -> data instanceof SwitchSensorAvro s
                ? Optional.of(s.getState() ? 1 : 0) : Optional.empty();
            case CO2LEVEL -> data instanceof ClimateSensorAvro c
                ? Optional.of(c.getCo2Level()) : Optional.empty();
            case HUMIDITY -> data instanceof ClimateSensorAvro c
                ? Optional.of(c.getHumidity()) : Optional.empty();
        };
    }

    private boolean compare(ConditionOperation operation, int actual, int expected) {
        return switch (operation) {
            case EQUALS -> actual == expected;
            case GREATER_THAN -> actual > expected;
            case LOWER_THAN -> actual < expected;
        };
    }
}