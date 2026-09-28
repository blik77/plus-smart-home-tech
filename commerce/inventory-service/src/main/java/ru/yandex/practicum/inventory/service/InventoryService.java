package ru.yandex.practicum.inventory.service;

import ru.yandex.practicum.inventory.dto.*;

import java.util.List;

public interface InventoryService {

    List<InventoryDto> getAll();
    InventoryDto getByProductId(Long productId);
    InventoryDto create(UpdateInventoryRequest request);
    InventoryDto updateQuantity(UpdateInventoryRequest request);
    ReserveResponse reserve(ReserveRequest request);

    ReserveResponse release(ReleaseRequest request);
}
