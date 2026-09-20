package ru.yandex.practicum.inventory.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.inventory.dto.InventoryDto;
import ru.yandex.practicum.inventory.dto.ReserveRequest;
import ru.yandex.practicum.inventory.dto.ReserveResponse;
import ru.yandex.practicum.inventory.dto.UpdateInventoryRequest;
import ru.yandex.practicum.inventory.entity.Inventory;
import ru.yandex.practicum.inventory.exception.InventoryConflictException;
import ru.yandex.practicum.inventory.exception.InsufficientStockException;
import ru.yandex.practicum.inventory.exception.NotFoundException;
import ru.yandex.practicum.inventory.mapper.InventoryMapper;
import ru.yandex.practicum.inventory.repository.InventoryRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;

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

        item.setQuantity(request.quantity());
        return inventoryMapper.toDto(inventoryRepository.save(item));
    }

    @Override
    @Transactional
    public ReserveResponse reserve(ReserveRequest request) {
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