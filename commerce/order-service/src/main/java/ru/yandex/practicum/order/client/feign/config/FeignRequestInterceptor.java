package ru.yandex.practicum.order.client.feign.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class FeignRequestInterceptor implements RequestInterceptor {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String SOURCE_SERVICE_HEADER = "X-Source-Service";

    @Override
    public void apply(RequestTemplate template) {
        // Передаём идентификатор сервиса
        template.header(SOURCE_SERVICE_HEADER, "order-service");

        // Передаём или создаём X-Request-Id для трассировки
        String requestId = MDC.get(REQUEST_ID_HEADER);
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }
        template.header(REQUEST_ID_HEADER, requestId);
    }
}