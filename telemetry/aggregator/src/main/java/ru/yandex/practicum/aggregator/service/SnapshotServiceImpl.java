package ru.yandex.practicum.aggregator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SnapshotServiceImpl implements SnapshotService {

    private final Map<String, SensorsSnapshotAvro> snapshots = new ConcurrentHashMap<>();

    @Override
    public Optional<SensorsSnapshotAvro> updateState(SensorEventAvro event) {
        String hubId = event.getHubId();
        String sensorId = event.getId();

        log.debug("Обработка события от датчика {} хаба {}", sensorId, hubId);

        SensorsSnapshotAvro snapshot = snapshots.computeIfAbsent(hubId, id ->
            SensorsSnapshotAvro.newBuilder()
                .setHubId(id)
                .setTimestamp(event.getTimestamp())
                .setSensorsState(new HashMap<>())
                .build()
        );

        SensorStateAvro oldState = snapshot.getSensorsState().get(sensorId);

        if (oldState != null) {
            if (oldState.getTimestamp().isAfter(event.getTimestamp())) {
                log.debug("Игнорируем устаревшее событие: old={}, new={}",
                    oldState.getTimestamp(), event.getTimestamp());
                return Optional.empty();
            }

            if (oldState.getData().equals(event.getPayload())) {
                log.debug("Игнорируем дубликат события для датчика {}", sensorId);
                return Optional.empty();
            }
        }

        SensorStateAvro newState = SensorStateAvro.newBuilder()
            .setTimestamp(event.getTimestamp())
            .setData(event.getPayload())
            .build();

        Map<String, SensorStateAvro> newSensorsState = new HashMap<>(snapshot.getSensorsState());
        newSensorsState.put(sensorId, newState);

        SensorsSnapshotAvro updatedSnapshot = SensorsSnapshotAvro.newBuilder()
            .setHubId(snapshot.getHubId())
            .setTimestamp(event.getTimestamp())
            .setSensorsState(newSensorsState)
            .build();

        snapshots.put(hubId, updatedSnapshot);

        log.info("Обновлён снапшот хаба {}: {} датчиков", hubId, newSensorsState.size());

        return Optional.of(updatedSnapshot);
    }
}