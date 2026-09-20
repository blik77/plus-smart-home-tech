package ru.yandex.practicum.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.practicum.order.client.InventoryClient;
import ru.yandex.practicum.order.client.ProductClient;
import ru.yandex.practicum.order.client.ProductInfo;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.entity.Order;
import ru.yandex.practicum.order.entity.OrderItem;
import ru.yandex.practicum.order.exception.NotFoundException;
import ru.yandex.practicum.order.mapper.OrderMapper;
import ru.yandex.practicum.order.repository.OrderRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final TransactionTemplate transactionTemplate;

    @Override
    public OrderDto create(CreateOrderRequest request) {

        Order order = orderMapper.toEntity(request);

        for (OrderItem item : order.getItems()) {
            inventoryClient.reserve(item.getProductId(), item.getQuantity());
        }

        for (OrderItem item : order.getItems()) {
            if (item.getProductName() == null || item.getPrice() == null) {
                log.info("Дозаполнение данных для товара {} из product-service", item.getProductId());
                ProductInfo info = productClient.getProduct(item.getProductId());

                if (info == null) {
                    throw new NotFoundException("Товар не найден в каталоге: " + item.getProductId());
                }

                if (item.getProductName() == null) {
                    item.setProductName(info.name());
                }
                if (item.getPrice() == null) {
                    item.setPrice(info.price());
                }
            }
        }

        order.setTotalPrice(orderMapper.calculateTotalPrice(order.getItems()));

        return transactionTemplate.execute(status -> {
            Order savedOrder = orderRepository.save(order);
            log.info("Создан заказ id={}, totalPrice={}", savedOrder.getId(), savedOrder.getTotalPrice());
            return orderMapper.toDto(savedOrder);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto getById(Long id) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Заказ не найден: " + id));
        return orderMapper.toDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> getAll() {
        return orderRepository.findAll().stream()
            .map(orderMapper::toDto)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> getByEmail(String email) {
        return orderRepository.findByCustomerEmailOrderByCreatedAtDesc(email).stream()
            .map(orderMapper::toDto)
            .toList();
    }
}