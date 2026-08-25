package ru.yandex.practicum.analyzer.processor;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;

import java.time.Duration;
import java.util.List;

@Slf4j
public abstract class BaseKafkaProcessor<K, V> implements Runnable {

    private final KafkaConsumer<K, V> consumer;
    private final String processorName;
    private final List<String> topics;
    private volatile Thread loopThread;

    protected BaseKafkaProcessor(KafkaConsumer<K, V> consumer, String processorName, List<String> topics) {
        this.consumer = consumer;
        this.processorName = processorName;
        this.topics = topics;
    }

    @Override
    public final void run() {
        loopThread = Thread.currentThread();
        log.info("{}: подписка на топики {}", processorName, topics);
        consumer.subscribe(topics);
        try {
            while (true) {
                handleRecords(consumer.poll(Duration.ofMillis(100)));
            }
        } catch (WakeupException e) {
            log.info("{}: получен сигнал остановки", processorName);
        } catch (Exception e) {
            log.error("{}: ошибка обработки сообщений", processorName, e);
        } finally {
            try {
                onShutdown();
            } finally {
                consumer.close();
                log.info("{}: consumer закрыт", processorName);
            }
        }
    }

    @PreDestroy
    public final void stop() {
        log.info("{}: @PreDestroy, прерываем poll loop", processorName);
        consumer.wakeup();
        Thread t = loopThread;
        if (t != null && t != Thread.currentThread()) {
            try {
                t.join(10_000);
                log.info("{}: poll loop завершён", processorName);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    protected final void commitOffsets() {
        consumer.commitSync();
    }

    protected void onShutdown() {
        // хук для наследников (например, финальный commit оффсетов)
    }

    protected abstract void handleRecords(ConsumerRecords<K, V> records);
}