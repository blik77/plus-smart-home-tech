package ru.yandex.practicum.collector.service;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.collector.config.KafkaProperties;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService implements AutoCloseable {

    private final KafkaProducer<String, SpecificRecordBase> kafkaProducer;
    private final KafkaProperties kafkaProperties;

    public void sendSensorEvent(SensorEventAvro event, String key) {
        Long timestamp = event.getTimestamp().toEpochMilli();
        ProducerRecord<String, SpecificRecordBase> record = new ProducerRecord<>(
            kafkaProperties.getSensorTopic(), null, timestamp, key, event
        );

        try {
            Future<RecordMetadata> future = kafkaProducer.send(record);

            RecordMetadata metadata = future.get();
            log.info("Событие датчика успешно сохранено: topic={}, partition={}, offset={}",
                metadata.topic(), metadata.partition(), metadata.offset());

            kafkaProducer.flush();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Отправка события датчика была прервана", e);
            throw new RuntimeException("Ошибка при отправке события датчика", e);
        } catch (ExecutionException e) {
            log.error("Ошибка при сохранении события датчика в Kafka", e);
            throw new RuntimeException("Ошибка при отправке события датчика", e);
        }
    }

    public void sendHubEvent(HubEventAvro event, String key) {
        Long timestamp = event.getTimestamp().toEpochMilli();
        ProducerRecord<String, SpecificRecordBase> record = new ProducerRecord<>(
            kafkaProperties.getHubTopic(), null, timestamp, key, event
        );

        try {
            Future<RecordMetadata> future = kafkaProducer.send(record);
            RecordMetadata metadata = future.get();
            log.info("Событие хаба успешно сохранено: topic={}, partition={}, offset={}",
                metadata.topic(), metadata.partition(), metadata.offset());

            kafkaProducer.flush();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Отправка события хаба была прервана", e);
            throw new RuntimeException("Ошибка при отправке события хаба", e);
        } catch (ExecutionException e) {
            log.error("Ошибка при сохранении события хаба в Kafka", e);
            throw new RuntimeException("Ошибка при отправке события хаба", e);
        }
    }

    @PreDestroy
    @Override
    public void close() {
        log.info("Начинается корректное завершение работы KafkaProducer...");
        try {
            kafkaProducer.flush();
            kafkaProducer.close(Duration.ofSeconds(10));
            log.info("KafkaProducer успешно и корректно закрыт");
        } catch (Exception e) {
            log.error("Ошибка при закрытии KafkaProducer", e);
        }
    }
}