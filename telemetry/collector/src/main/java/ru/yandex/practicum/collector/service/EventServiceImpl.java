package ru.yandex.practicum.collector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.collector.dto.hub.HubEventDto;
import ru.yandex.practicum.collector.dto.sensor.SensorEventDto;
import ru.yandex.practicum.collector.mapper.EventMapper;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventMapper eventMapper;
    private final KafkaProducerService kafkaProducerService;

    @Override
    public void processSensorEvent(SensorEventDto dto) {
        log.info("Обработка события от сенсора. Type: {}, ID: {}, HubId: {}",
            dto.getType(), dto.getId(), dto.getHubId());

        SensorEventAvro avroEvent = eventMapper.toAvro(dto);
        kafkaProducerService.sendSensorEvent(avroEvent, dto.getId());
    }

    @Override
    public void processHubEvent(HubEventDto dto) {
        log.info("Обработка события от хаба. Type: {}, HubId: {}", dto.getType(), dto.getHubId());

        HubEventAvro avroEvent = eventMapper.toAvro(dto);
        kafkaProducerService.sendHubEvent(avroEvent, dto.getHubId());
    }
}