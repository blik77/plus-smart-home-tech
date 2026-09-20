package ru.yandex.practicum.order.client;

import java.math.BigDecimal;

public record ProductInfo(Long id, String name, BigDecimal price) {

}