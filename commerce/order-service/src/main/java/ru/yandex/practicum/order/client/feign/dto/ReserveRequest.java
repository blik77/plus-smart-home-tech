package ru.yandex.practicum.order.client.feign.dto;

public record ReserveRequest(
    Long productId,
    Integer quantity
) {
}