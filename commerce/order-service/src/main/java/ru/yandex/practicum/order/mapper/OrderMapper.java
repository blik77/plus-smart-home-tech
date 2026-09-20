package ru.yandex.practicum.order.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.dto.OrderItemDto;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.entity.Order;
import ru.yandex.practicum.order.entity.OrderItem;
import ru.yandex.practicum.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class OrderMapper {

    public Order toEntity(CreateOrderRequest request) {
        Order order = new Order();
        order.setCustomerName(request.customerName());
        order.setCustomerEmail(request.customerEmail());
        order.setStatus(OrderStatus.CREATED);
        order.setCreatedAt(LocalDateTime.now());

        for (OrderItemRequest itemRequest : request.items()) {
            OrderItem item = new OrderItem();
            item.setProductId(itemRequest.productId());
            item.setProductName(itemRequest.productName());
            item.setQuantity(itemRequest.quantity());
            item.setPrice(itemRequest.price());
            order.addItem(item);
        }

        return order;
    }

    public BigDecimal calculateTotalPrice(List<OrderItem> items) {
        return items.stream()
            .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public OrderDto toDto(Order order) {
        return new OrderDto(
            order.getId(),
            order.getCustomerName(),
            order.getCustomerEmail(),
            order.getStatus().name(),
            order.getTotalPrice(),
            order.getStatusDetails(),
            order.getCreatedAt(),
            order.getItems().stream()
                .map(this::toItemDto)
                .toList()
        );
    }

    public OrderItemDto toItemDto(OrderItem item) {
        return new OrderItemDto(
            item.getId(),
            item.getProductId(),
            item.getProductName(),
            item.getQuantity(),
            item.getPrice()
        );
    }
}