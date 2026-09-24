package ru.yandex.practicum.order.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductClient {

    private final RestTemplate restTemplate;

    public ProductInfo getProduct(Long productId) {

        String url = "http://product-service/api/products/" + productId;
        log.info("Запрос информации о товаре из product-service: {}", url);
        try {
            return restTemplate.getForObject(url, ProductInfo.class);
        } catch (Exception e) {
            log.error("Не удалось получить товар {} из product-service", productId, e);
            return null;
        }
    }
}