package ru.yandex.practicum.order.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductClient {

    private static final String PRODUCT_URL_TEMPLATE = "http://product-service/api/products/%d";

    private final RestTemplate restTemplate;

    public ProductInfo getProduct(Long productId) {

        log.info("Запрос информации о товаре из product-service: {}", PRODUCT_URL_TEMPLATE);
        try {
            return restTemplate.getForObject(PRODUCT_URL_TEMPLATE, ProductInfo.class);
        } catch (Exception e) {
            log.error("Не удалось получить товар {} из product-service", productId, e);
            return null;
        }
    }
}