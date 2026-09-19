package ru.yandex.practicum.analyzer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "analyzer.kafka")
public class KafkaProperties {

    private String bootstrapServers;
    private Topics topics = new Topics();
    private Consumer consumer = new Consumer();

    @Getter
    @Setter
    public static class Topics {
        private String snapshots;
        private String hubEvents;
    }

    @Getter
    @Setter
    public static class Consumer {
        private String snapshotsGroup;
        private String hubEventsGroup;
        private String enableAutoCommit;
        private String autoOffsetReset;
    }
}