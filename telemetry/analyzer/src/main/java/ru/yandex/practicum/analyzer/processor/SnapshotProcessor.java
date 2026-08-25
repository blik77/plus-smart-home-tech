package ru.yandex.practicum.analyzer.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.config.KafkaProperties;
import ru.yandex.practicum.analyzer.model.ActionToExecute;
import ru.yandex.practicum.analyzer.model.entity.Scenario;
import ru.yandex.practicum.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.analyzer.service.ActionExecutor;
import ru.yandex.practicum.analyzer.service.ScenarioAnalyzer;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.util.List;

@Slf4j
@Component
public class SnapshotProcessor extends BaseKafkaProcessor<String, SensorsSnapshotAvro> {

    private final ScenarioRepository scenarioRepository;
    private final ScenarioAnalyzer scenarioAnalyzer;
    private final ActionExecutor actionExecutor;

    public SnapshotProcessor(KafkaConsumer<String, SensorsSnapshotAvro> snapshotConsumer,
                             ScenarioRepository scenarioRepository,
                             ScenarioAnalyzer scenarioAnalyzer,
                             ActionExecutor actionExecutor,
                             KafkaProperties kafkaProperties) {
        super(snapshotConsumer, "SnapshotProcessor",
            List.of(kafkaProperties.getTopics().getSnapshots()));
        this.scenarioRepository = scenarioRepository;
        this.scenarioAnalyzer = scenarioAnalyzer;
        this.actionExecutor = actionExecutor;
    }

    @Override
    protected void handleRecords(ConsumerRecords<String, SensorsSnapshotAvro> records) {
        for (ConsumerRecord<String, SensorsSnapshotAvro> record : records) {
            processSnapshot(record.value());
        }

        if (!records.isEmpty()) {
            commitOffsets();
        }
    }

    @Override
    protected void onShutdown() {
        try {
            commitOffsets();
        } catch (Exception e) {
            log.warn("SnapshotProcessor: не удалось закоммитить оффсеты при завершении", e);
        }
    }

    private void processSnapshot(SensorsSnapshotAvro snapshot) {
        String hubId = snapshot.getHubId().toString();
        log.debug("SnapshotProcessor: снапшот хаба {}, датчиков: {}",
            hubId, snapshot.getSensorsState().size());

        List<Scenario> scenarios = scenarioRepository.findByHubId(hubId);
        if (scenarios.isEmpty()) {
            log.debug("SnapshotProcessor: нет сценариев для хаба {}, пропускаем снапшот", hubId);
            return;
        }

        List<ActionToExecute> actions = scenarioAnalyzer.analyze(snapshot, scenarios);
        actions.forEach(actionExecutor::execute);
    }
}