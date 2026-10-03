package ru.yandex.practicum.order.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.order.client.feign.InventoryClient;
import ru.yandex.practicum.order.client.feign.ProductClient;
import ru.yandex.practicum.order.client.feign.dto.ProductDto;
import ru.yandex.practicum.order.client.feign.dto.ReleaseRequest;
import ru.yandex.practicum.order.client.feign.dto.ReserveRequest;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.entity.Order;
import ru.yandex.practicum.order.entity.OrderItem;
import ru.yandex.practicum.order.entity.OrderStatus;
import ru.yandex.practicum.order.exception.InventoryServiceUnavailableException;
import ru.yandex.practicum.order.exception.NotFoundException;
import ru.yandex.practicum.order.exception.OrderProcessingException;
import ru.yandex.practicum.order.exception.ProductServiceUnavailableException;
import ru.yandex.practicum.order.mapper.OrderMapper;
import ru.yandex.practicum.order.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;

    @Override
    public OrderDto create(CreateOrderRequest request) {

        List<String> degradationReasons = new ArrayList<>();

        Map<Long, ServiceCallResult<ProductDto>> productResults = new LinkedHashMap<>();
        for (OrderItemRequest itemRequest : request.items()) {
            productResults.computeIfAbsent(itemRequest.productId(), this::fetchProduct);
        }

        for (Map.Entry<Long, ServiceCallResult<ProductDto>> entry : productResults.entrySet()) {
            ServiceCallResult<ProductDto> result = entry.getValue();
            if (result.isDegraded()) {
                degradationReasons.add(result.degradationReason());
            } else if (!Boolean.TRUE.equals(result.data().active())) {
                throw new OrderProcessingException(
                    "Товар снят с продажи и недоступен для заказа: " + entry.getKey());
            }
        }

        Map<Long, Integer> totalQuantities = new LinkedHashMap<>();
        for (OrderItemRequest itemRequest : request.items()) {
            totalQuantities.merge(itemRequest.productId(), itemRequest.quantity(), Integer::sum);
        }

        List<ReserveRequest> successfulReservations = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : totalQuantities.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();
            log.info("Начинаем резервирование: productId={}, quantity={}", productId, quantity);

            ReserveRequest reserveRequest = new ReserveRequest(productId, quantity);
            try {
                inventoryClient.reserveStock(reserveRequest);
                successfulReservations.add(reserveRequest);
                log.info("Зарезервирован товар {} в количестве {}", productId, quantity);
            } catch (InventoryServiceUnavailableException e) {
                log.warn("Склад недоступен для товара {}: резерв не подтверждён", productId);
                degradationReasons.add(e.getMessage());
            } catch (FeignException e) {
                compensate(successfulReservations);
                throw convertReserveError(e, productId);
            }
        }

        boolean degraded = !degradationReasons.isEmpty();

        Order order = orderMapper.toEntity(request);
        order.setStatus(degraded ? OrderStatus.PENDING_CONFIRMATION : OrderStatus.CONFIRMED);

        for (OrderItem item : order.getItems()) {
            ServiceCallResult<ProductDto> result = productResults.get(item.getProductId());
            if (result.isSuccess()) {
                item.setProductName(result.data().name());
                item.setPrice(result.data().price());
            } else {
                item.setProductName(
                        String.format("Товар #%d (ожидает проверки)", item.getProductId()));
                item.setPrice(BigDecimal.ZERO);
            }
        }
        order.setTotalPrice(orderMapper.calculateTotalPrice(order.getItems()));

        if (degraded) {
            order.setStatusDetails("Заказ требует ручной проверки: "
                    + String.join("; ", degradationReasons));
        }

        try {
            Order savedOrder = orderRepository.save(order);
            log.info("Создан заказ id={}, статус={}, totalPrice={}",
                savedOrder.getId(), savedOrder.getStatus(), savedOrder.getTotalPrice());
            return orderMapper.toDto(savedOrder);
        } catch (Exception e) {
            compensate(successfulReservations);
            throw new OrderProcessingException("Не удалось сохранить заказ: " + e.getMessage());
        }
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

    private ServiceCallResult<ProductDto> fetchProduct(Long productId) {
        try {
            return ServiceCallResult.success(productClient.getProductById(productId));
        } catch (ProductServiceUnavailableException e) {
            log.warn("Каталог недоступен, товар {} будет принят на проверку", productId);
            return ServiceCallResult.degraded(e.getMessage());
        } catch (FeignException.NotFound e) {
            throw new OrderProcessingException("Товар не найден в каталоге: " + productId);
        } catch (FeignException e) {
            throw new OrderProcessingException(
                "Каталог отклонил запрос товара: " + productId);
        }
    }

    private OrderProcessingException convertReserveError(FeignException e, Long productId) {
        if (e instanceof FeignException.NotFound) {
            return new OrderProcessingException(
                "Складская запись не найдена для товара: " + productId);
        }
        if (e instanceof FeignException.Conflict) {
            return new OrderProcessingException(
                "Недостаточно товара на складе: " + productId);
        }
        return new OrderProcessingException("Не удалось зарезервировать товар: " + productId);
    }

    private void compensate(List<ReserveRequest> successfulReservations) {
        for (ReserveRequest reservation : successfulReservations) {
            try {
                inventoryClient.releaseStock(
                    new ReleaseRequest(reservation.productId(), reservation.quantity()));
                log.info("Снят резерв: товар {}, количество {}",
                    reservation.productId(), reservation.quantity());
            } catch (Exception e) {
                log.error("НЕ УДАЛОСЬ снять резерв для товара {}: {}",
                    reservation.productId(), e.getMessage());
            }
        }
    }
}