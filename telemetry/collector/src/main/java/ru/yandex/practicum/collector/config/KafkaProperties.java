package ru.yandex.practicum.collector.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "collector.kafka")
public class KafkaProperties {
    private String sensorTopic = "telemetry.sensors.v1";
    private String hubTopic = "telemetry.hubs.v1";
    private String bootstrapServers = "localhost:9092";
    private String keySerializer = "org.apache.kafka.common.serialization.StringSerializer";
    private String valueSerializer = "ru.yandex.practicum.collector.serialization.GeneralAvroSerializer";
}