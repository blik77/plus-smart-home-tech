package ru.yandex.practicum.collector.service;

import ru.yandex.practicum.collector.dto.hub.HubEventDto;
import ru.yandex.practicum.collector.dto.sensor.SensorEventDto;

public interface EventService {

    void processSensorEvent(SensorEventDto dto);

    void processHubEvent(HubEventDto dto);

}
