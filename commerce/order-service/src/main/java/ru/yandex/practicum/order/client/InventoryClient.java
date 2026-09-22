package ru.yandex.practicum.order.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ru.yandex.practicum.order.exception.InsufficientStockException;
import ru.yandex.practicum.order.exception.NotFoundException;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryClient {

    private static final String RESERVE_URL = "http://inventory-service/api/inventory/reserve";

    private final RestTemplate restTemplate;

    public void reserve(Long productId, Integer quantity) {
        ReserveRequest request = new ReserveRequest(productId, quantity);

        try {
            log.info("Запрос на резервирование: товар={}, количество={}", productId, quantity);
            restTemplate.postForEntity(RESERVE_URL, request, String.class);
            log.info("Товар {} успешно зарезервирован на складе", productId);
        } catch (HttpClientErrorException.Conflict e) {
            log.warn("Склад вернул 409 Conflict для товара {}: {}", productId, e.getResponseBodyAsString());
            throw new InsufficientStockException("Недостаточно товара на складе (ID: " + productId + ")");
        } catch (Exception e) {
            log.error("Ошибка связи со складом для товара {}", productId, e);
            throw new NotFoundException("Ошибка связи со складом для товара (ID: " + productId + ")");
        }
    }

    public record ReserveRequest(Long productId, Integer quantity) {}
}