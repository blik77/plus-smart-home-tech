package ru.yandex.practicum.inventory.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.inventory.dto.*;
import ru.yandex.practicum.inventory.entity.Inventory;
import ru.yandex.practicum.inventory.exception.InventoryConflictException;
import ru.yandex.practicum.inventory.exception.InsufficientStockException;
import ru.yandex.practicum.inventory.exception.NotFoundException;
import ru.yandex.practicum.inventory.mapper.InventoryMapper;
import ru.yandex.practicum.inventory.repository.InventoryRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;

    @Autowired
    private HttpServletRequest requester;

    @Override
    @Transactional
    public ReserveResponse release(ReleaseRequest request) {
        Inventory item = inventoryRepository.findByProductId(request.productId())
                .orElseThrow(() -> new NotFoundException(
                        "Складская запись не найдена для товара: " + request.productId()));

        if (item.getReservedQuantity() < request.quantity()) {
            throw new IllegalArgumentException(String.format(
                    "Невозможно снять резерв: товар %d, зарезервировано %d, запрошено к снятию %d",
                    item.getProductId(),
                    item.getReservedQuantity(),
                    request.quantity()));
        }

        item.setReservedQuantity(item.getReservedQuantity() - request.quantity());
        Inventory saved = inventoryRepository.save(item);

        return new ReserveResponse(
                true,
                saved.getAvailableQuantity(),
                String.format("Снят резерв %d шт. для товара %d",
                        request.quantity(), request.productId())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryDto> getAll() {
        return inventoryRepository.findAll().stream()
            .map(inventoryMapper::toDto)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryDto getByProductId(Long productId) {
        Inventory item = inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new NotFoundException("Складская запись не найдена для товара: " + productId));
        return inventoryMapper.toDto(item);
    }

    @Override
    @Transactional
    public InventoryDto create(UpdateInventoryRequest request) {
        if (inventoryRepository.existsByProductId(request.productId())) {
            throw new InventoryConflictException(
                "Складская запись уже существует для товара: " + request.productId());
        }

        Inventory item = new Inventory();
        item.setProductId(request.productId());
        item.setQuantity(request.quantity());
        item.setReservedQuantity(0);

        return inventoryMapper.toDto(inventoryRepository.save(item));
    }

    @Override
    @Transactional
    public InventoryDto updateQuantity(UpdateInventoryRequest request) {
        Inventory item = inventoryRepository.findByProductId(request.productId())
            .orElseThrow(() -> new NotFoundException("Складская запись не найдена для товара: " + request.productId()));

        if (request.quantity() < item.getReservedQuantity()) {
            throw new InsufficientStockException(String.format(
                    "Недостаточно товара на складе: товар %d, зарезервировано %d, новое кол-во %d",
                    item.getProductId(), item.getReservedQuantity(), request.quantity()));
        }

        item.setQuantity(request.quantity());
        return inventoryMapper.toDto(inventoryRepository.save(item));
    }

    @Override
    @Transactional
    public ReserveResponse reserve(ReserveRequest request) {
        log.info("Получены заголовки: X-Source-Service={}, X-Request-Id={}",
                requester.getHeader("X-Source-Service"),
                requester.getHeader("X-Request-Id"));
        Inventory item = inventoryRepository.findByProductId(request.productId())
            .orElseThrow(() -> new NotFoundException("Складская запись не найдена для товара: " + request.productId()));

        if (item.getAvailableQuantity() < request.quantity()) {
            throw new InsufficientStockException(String.format(
                "Недостаточно товара на складе: товар %d, доступно %d, запрошено %d",
                item.getProductId(), item.getAvailableQuantity(), request.quantity()));
        }

        item.setReservedQuantity(item.getReservedQuantity() + request.quantity());
        Inventory saved = inventoryRepository.save(item);

        return new ReserveResponse(
            true,
            saved.getAvailableQuantity(),
            String.format("Зарезервировано %d шт. для товара %d",
                request.quantity(), request.productId())
        );
    }
}