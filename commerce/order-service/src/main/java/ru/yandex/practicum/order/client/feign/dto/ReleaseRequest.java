package ru.yandex.practicum.order.client.feign.dto;

public record ReleaseRequest(
    Long productId,
    Integer quantity
) {
}
