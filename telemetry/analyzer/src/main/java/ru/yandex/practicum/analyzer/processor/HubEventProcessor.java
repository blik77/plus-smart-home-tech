package ru.yandex.practicum.analyzer.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.config.KafkaProperties;
import ru.yandex.practicum.analyzer.service.HubEventService;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.util.List;

@Slf4j
@Component
public class HubEventProcessor extends BaseKafkaProcessor<String, HubEventAvro> {

    private final HubEventService hubEventService;

    public HubEventProcessor(
        KafkaConsumer<String, HubEventAvro> hubEventConsumer,
        HubEventService hubEventService,
        KafkaProperties kafkaProperties) {
        super(hubEventConsumer, "HubEventProcessor",
            List.of(kafkaProperties.getTopics().getHubEvents()));
        this.hubEventService = hubEventService;
    }

    @Override
    protected void handleRecords(ConsumerRecords<String, HubEventAvro> records) {
        for (ConsumerRecord<String, HubEventAvro> record : records) {
            handle(record.value());
        }

        if (!records.isEmpty()) {
            commitOffsets();
        }
    }

    private void handle(HubEventAvro event) {
        String hubId = event.getHubId().toString();
        Object payload = event.getPayload();
        log.debug("HubEventProcessor: событие хаба {}: {}", hubId,
            payload.getClass().getSimpleName());

        if (payload instanceof DeviceAddedEventAvro added) {
            hubEventService.addDevice(hubId, added);
        } else if (payload instanceof DeviceRemovedEventAvro removed) {
            hubEventService.removeDevice(hubId, removed.getId().toString());
        } else if (payload instanceof ScenarioAddedEventAvro added) {
            hubEventService.addScenario(hubId, added);
        } else if (payload instanceof ScenarioRemovedEventAvro removed) {
            hubEventService.removeScenario(hubId, removed.getName().toString());
        } else {
            log.warn("HubEventProcessor: неизвестный тип события {}", payload.getClass());
        }
    }
}