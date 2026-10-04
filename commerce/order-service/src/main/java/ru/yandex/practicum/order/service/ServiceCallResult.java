package ru.yandex.practicum.order.service;

public record ServiceCallResult<T>(
    CallStatus status,
    T data,
    String degradationReason
) {

    public enum CallStatus {
        SUCCESS,    // соседний сервис ответил успешно
        DEGRADED    // сервис технически недоступен
    }

    public static <T> ServiceCallResult<T> success(T data) {
        return new ServiceCallResult<>(CallStatus.SUCCESS, data, null);
    }

    public static <T> ServiceCallResult<T> degraded(String reason) {
        return new ServiceCallResult<>(CallStatus.DEGRADED, null, reason);
    }

    public boolean isSuccess() {
        return status == CallStatus.SUCCESS;
    }

    public boolean isDegraded() {
        return status == CallStatus.DEGRADED;
    }
}