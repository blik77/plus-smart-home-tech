package ru.yandex.practicum.collector.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Instant;

@Slf4j
@Component
public class GrpcToAvroMapper {

    public SensorEventAvro toAvro(SensorEventProto grpcEvent) {
        Instant timestamp = Instant.ofEpochSecond(
            grpcEvent.getTimestamp().getSeconds(),
            grpcEvent.getTimestamp().getNanos()
        );

        SensorEventAvro.Builder builder = SensorEventAvro.newBuilder()
            .setId(grpcEvent.getId())
            .setHubId(grpcEvent.getHubId())
            .setTimestamp(timestamp);

        switch (grpcEvent.getPayloadCase()) {
            case MOTION_SENSOR -> {
                MotionSensorProto motion = grpcEvent.getMotionSensor();
                builder.setPayload(MotionSensorAvro.newBuilder()
                    .setLinkQuality(motion.getLinkQuality())
                    .setMotion(motion.getMotion())
                    .setVoltage(motion.getVoltage())
                    .build());
            }
            case TEMPERATURE_SENSOR -> {
                TemperatureSensorProto temp = grpcEvent.getTemperatureSensor();
                builder.setPayload(TemperatureSensorAvro.newBuilder()
                    .setId(grpcEvent.getId())
                    .setHubId(grpcEvent.getHubId())
                    .setTimestamp(timestamp)
                    .setTemperatureC(temp.getTemperatureC())
                    .setTemperatureF(temp.getTemperatureF())
                    .build());
            }
            case LIGHT_SENSOR -> {
                LightSensorProto light = grpcEvent.getLightSensor();
                builder.setPayload(LightSensorAvro.newBuilder()
                    .setLinkQuality(light.getLinkQuality())
                    .setLuminosity(light.getLuminosity())
                    .build());
            }
            case CLIMATE_SENSOR -> {
                ClimateSensorProto climate = grpcEvent.getClimateSensor();
                builder.setPayload(ClimateSensorAvro.newBuilder()
                    .setTemperatureC(climate.getTemperatureC())
                    .setHumidity(climate.getHumidity())
                    .setCo2Level(climate.getCo2Level())
                    .build());
            }
            case SWITCH_SENSOR -> {
                SwitchSensorProto switchSensor = grpcEvent.getSwitchSensor();
                builder.setPayload(SwitchSensorAvro.newBuilder()
                    .setState(switchSensor.getState())
                    .build());
            }
            case PAYLOAD_NOT_SET -> {
                log.warn("Payload не установлен в событии от сенсора: {}", grpcEvent.getId());
                throw new IllegalArgumentException("Payload не установлен");
            }
        }

        return builder.build();
    }

    public HubEventAvro toAvro(HubEventProto grpcEvent) {
        Instant timestamp = Instant.ofEpochSecond(
            grpcEvent.getTimestamp().getSeconds(),
            grpcEvent.getTimestamp().getNanos()
        );

        HubEventAvro.Builder builder = HubEventAvro.newBuilder()
            .setHubId(grpcEvent.getHubId())
            .setTimestamp(timestamp);

        switch (grpcEvent.getPayloadCase()) {
            case DEVICE_ADDED -> {
                DeviceAddedEventProto added = grpcEvent.getDeviceAdded();
                builder.setPayload(DeviceAddedEventAvro.newBuilder()
                    .setId(added.getId())
                    .setType(DeviceTypeAvro.valueOf(added.getType().name()))
                    .build());
            }
            case DEVICE_REMOVED -> {
                DeviceRemovedEventProto removed = grpcEvent.getDeviceRemoved();
                builder.setPayload(DeviceRemovedEventAvro.newBuilder()
                    .setId(removed.getId())
                    .build());
            }
            case SCENARIO_ADDED -> {
                ScenarioAddedEventProto added = grpcEvent.getScenarioAdded();
                builder.setPayload(ScenarioAddedEventAvro.newBuilder()
                    .setName(added.getName())
                    .setConditions(added.getConditionList().stream()
                        .map(this::toAvroCondition)
                        .toList())
                    .setActions(added.getActionList().stream()
                        .map(this::toAvroAction)
                        .toList())
                    .build());
            }
            case SCENARIO_REMOVED -> {
                ScenarioRemovedEventProto removed = grpcEvent.getScenarioRemoved();
                builder.setPayload(ScenarioRemovedEventAvro.newBuilder()
                    .setName(removed.getName())
                    .build());
            }
            case PAYLOAD_NOT_SET -> {
                log.warn("Payload не установлен в событии от хаба: {}", grpcEvent.getHubId());
                throw new IllegalArgumentException("Payload не установлен");
            }
        }

        return builder.build();
    }

    private ScenarioConditionAvro toAvroCondition(ScenarioConditionProto c) {
        ScenarioConditionAvro.Builder b = ScenarioConditionAvro.newBuilder()
            .setSensorId(c.getSensorId())
            .setType(ConditionTypeAvro.valueOf(c.getType().name()))
            .setOperation(ConditionOperationAvro.valueOf(c.getOperation().name()));
        switch (c.getValueCase()) {
            case BOOL_VALUE -> b.setValue(c.getBoolValue());
            case INT_VALUE -> b.setValue(c.getIntValue());
            case VALUE_NOT_SET -> throw new IllegalArgumentException(
                "У условия сценария не задано значение");
        }
        return b.build();
    }

    private DeviceActionAvro toAvroAction(DeviceActionProto a) {
        return DeviceActionAvro.newBuilder()
            .setSensorId(a.getSensorId())
            .setType(ActionTypeAvro.valueOf(a.getType().name()))
            .setValue(a.hasValue() ? a.getValue() : null)
            .build();
    }
}