package ru.yandex.practicum.aggregator.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "aggregator.kafka")
public class KafkaProperties {

    private String bootstrapServers;
    private Consumer consumer = new Consumer();
    private Topics topics = new Topics();

    @Getter
    @Setter
    public static class Consumer {
        private String groupId;
        private String autoOffsetReset = "earliest";
        private boolean enableAutoCommit = false;
    }

    @Getter
    @Setter
    public static class Topics {
        private String input;
        private String output;
    }
}