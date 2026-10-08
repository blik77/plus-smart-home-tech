package ru.yandex.practicum.order.exception;

public class InventoryServiceUnavailableException extends RuntimeException {

    private final Long productId;

    public InventoryServiceUnavailableException(Long productId, Throwable cause) {
        super(String.format("Сервис склада технически недоступен для товара: %d", productId), cause);
        this.productId = productId;
    }

    public Long getProductId() {
        return productId;
    }
}