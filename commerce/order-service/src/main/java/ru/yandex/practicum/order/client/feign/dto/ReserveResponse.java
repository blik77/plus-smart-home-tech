package ru.yandex.practicum.order.client.feign.dto;

public record ReserveResponse(
    Long productId,
    Integer reservedQuantity,
    Integer availableQuantity
) {
}