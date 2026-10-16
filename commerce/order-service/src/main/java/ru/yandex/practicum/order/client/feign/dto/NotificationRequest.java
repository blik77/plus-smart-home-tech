package ru.yandex.practicum.order.client.feign.dto;

public record NotificationRequest(
    Long orderId,
    String email,
    String message
) {
}