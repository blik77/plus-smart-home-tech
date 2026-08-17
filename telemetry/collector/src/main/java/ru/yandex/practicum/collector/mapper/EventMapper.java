package ru.yandex.practicum.collector.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.dto.hub.*;
import ru.yandex.practicum.collector.dto.sensor.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

@Component
public class EventMapper {

    public SensorEventAvro toAvro(SensorEventDto dto) {
        SensorEventAvro.Builder builder = SensorEventAvro.newBuilder()
            .setId(dto.getId())
            .setHubId(dto.getHubId())
            .setTimestamp(dto.getTimestamp());

        switch (dto) {
            case ClimateSensorEventDto c -> builder.setPayload(ClimateSensorAvro.newBuilder()
                .setTemperatureC(c.getTemperatureC())
                .setHumidity(c.getHumidity())
                .setCo2Level(c.getCo2Level())
                .build());
            case LightSensorEventDto l -> builder.setPayload(LightSensorAvro.newBuilder()
                .setLinkQuality(l.getLinkQuality())
                .setLuminosity(l.getLuminosity())
                .build());
            case MotionSensorEventDto m -> builder.setPayload(MotionSensorAvro.newBuilder()
                .setLinkQuality(m.getLinkQuality())
                .setMotion(m.getMotion())
                .setVoltage(m.getVoltage())
                .build());
            case SwitchSensorEventDto s -> builder.setPayload(SwitchSensorAvro.newBuilder()
                .setState(s.getState())
                .build());
            case TemperatureSensorEventDto t -> builder.setPayload(TemperatureSensorAvro.newBuilder()
                .setId(t.getId())
                .setHubId(t.getHubId())
                .setTimestamp(t.getTimestamp())
                .setTemperatureC(t.getTemperatureC())
                .setTemperatureF(t.getTemperatureF())
                .build());
            default -> throw new IllegalArgumentException("Неизвестный тип сенсора: " + dto.getClass().getSimpleName());
        }
        return builder.build();
    }

    public HubEventAvro toAvro(HubEventDto dto) {
        HubEventAvro.Builder builder = HubEventAvro.newBuilder()
            .setHubId(dto.getHubId())
            .setTimestamp(dto.getTimestamp());

        switch (dto) {
            case DeviceAddedEventDto added -> builder.setPayload(DeviceAddedEventAvro.newBuilder()
                .setId(added.getId())
                .setType(DeviceTypeAvro.valueOf(added.getDeviceType()))
                .build());
            case DeviceRemovedEventDto removed -> builder.setPayload(DeviceRemovedEventAvro.newBuilder()
                .setId(removed.getId())
                .build());
            case ScenarioAddedEventDto added -> builder.setPayload(ScenarioAddedEventAvro.newBuilder()
                .setName(added.getName())
                .setConditions(added.getConditions().stream().map(c -> ScenarioConditionAvro.newBuilder()
                    .setSensorId(c.getSensorId())
                    .setType(ConditionTypeAvro.valueOf(c.getType()))
                    .setOperation(ConditionOperationAvro.valueOf(c.getOperation()))
                    .setValue(c.getValue())
                    .build()).toList())
                .setActions(added.getActions().stream().map(a -> DeviceActionAvro.newBuilder()
                    .setSensorId(a.getSensorId())
                    .setType(ActionTypeAvro.valueOf(a.getType()))
                    .setValue(a.getValue())
                    .build()).toList())
                .build());
            case ScenarioRemovedEventDto removed -> builder.setPayload(ScenarioRemovedEventAvro.newBuilder()
                .setName(removed.getName())
                .build());
            default -> throw new IllegalArgumentException("Неизвестный тип события: " + dto.getClass().getSimpleName());
        }
        return builder.build();
    }
}