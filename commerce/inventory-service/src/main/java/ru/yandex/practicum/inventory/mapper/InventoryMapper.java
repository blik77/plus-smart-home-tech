package ru.yandex.practicum.inventory.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.inventory.dto.InventoryDto;
import ru.yandex.practicum.inventory.entity.Inventory;

@Component
public class InventoryMapper {

    public InventoryDto toDto(Inventory item) {
        return new InventoryDto(
            item.getId(),
            item.getProductId(),
            item.getQuantity(),
            item.getReservedQuantity(),
            item.getAvailableQuantity()
        );
    }
}