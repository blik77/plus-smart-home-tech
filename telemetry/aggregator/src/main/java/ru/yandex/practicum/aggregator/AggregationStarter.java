package ru.yandex.practicum.aggregator;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.aggregator.service.SnapshotService;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {

    private final KafkaConsumer<String, SensorEventAvro> consumer;
    private final KafkaProducer<String, SensorsSnapshotAvro> producer;

    private final SnapshotService snapshotService;

    @Value("${aggregator.kafka.topics.input}")
    private String inputTopic;

    @Value("${aggregator.kafka.topics.output}")
    private String outputTopic;

    private volatile Thread mainLoopThread;

    public void start() {
        mainLoopThread = Thread.currentThread();
        log.info("Запуск Aggregator, подписка на топик: {}", inputTopic);

        try {
            consumer.subscribe(Collections.singletonList(inputTopic));

            while (true) {
                ConsumerRecords<String, SensorEventAvro> records = consumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<String, SensorEventAvro> record : records) {
                    processRecord(record);
                }

                consumer.commitSync();
            }
        } catch (WakeupException ignored) {
            log.info("Получен сигнал остановки (WakeupException)");
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от датчиков", e);
        } finally {
            shutdown();
        }
    }

    private void processRecord(ConsumerRecord<String, SensorEventAvro> record) {
        SensorEventAvro event = record.value();
        log.debug("Получено событие: hubId={}, sensorId={}, timestamp={}",
            event.getHubId(), event.getId(), event.getTimestamp());

        Optional<SensorsSnapshotAvro> updatedSnapshot = snapshotService.updateState(event);

        if (updatedSnapshot.isPresent()) {
            SensorsSnapshotAvro snapshot = updatedSnapshot.get();
            producer.send(new ProducerRecord<>(outputTopic, snapshot.getHubId(), snapshot),
                (metadata, exception) -> {
                    if (exception != null) {
                        log.error("Ошибка отправки снапшота в Kafka: {}", exception.getMessage());
                    } else {
                        log.debug("Снапшот отправлен: topic={}, partition={}, offset={}",
                            metadata.topic(), metadata.partition(), metadata.offset());
                    }
                });
        }
    }

    private void shutdown() {
        try {
            log.info("Завершение работы: flush producer...");
            producer.flush();

            log.info("Commit offsets...");
            consumer.commitSync();
        } catch (Exception e) {
            log.error("Ошибка при завершении работы", e);
        } finally {
            log.info("Закрываем consumer");
            consumer.close();
            log.info("Закрываем producer");
            producer.close();
        }
    }

    @PreDestroy
    public void stop() {
        log.info("Вызван @PreDestroy, прерываем poll loop");
        consumer.wakeup();
        try {
            if (mainLoopThread != null && mainLoopThread != Thread.currentThread()) {
                mainLoopThread.join(10_000);
                log.info("Poll loop завершён корректно");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}