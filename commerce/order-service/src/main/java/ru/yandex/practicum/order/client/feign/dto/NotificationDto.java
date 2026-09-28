package ru.yandex.practicum.order.client.feign.dto;

public record NotificationDto(
    Long id,
    Long orderId,
    String status
) {
}