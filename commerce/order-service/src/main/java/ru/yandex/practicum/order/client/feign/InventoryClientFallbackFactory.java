package ru.yandex.practicum.order.client.feign;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.order.client.feign.dto.ReleaseRequest;
import ru.yandex.practicum.order.client.feign.dto.ReserveRequest;
import ru.yandex.practicum.order.client.feign.dto.ReserveResponse;
import ru.yandex.practicum.order.exception.InventoryServiceUnavailableException;

@Slf4j
@Component
public class InventoryClientFallbackFactory implements FallbackFactory<InventoryClient> {

    @Override
    public InventoryClient create(Throwable cause) {
        log.warn("Fallback сработал для InventoryClient. Причина: {}", cause.getMessage());

        return new InventoryClient() {
            @Override
            public ReserveResponse reserveStock(ReserveRequest request) {
                // Бизнес-ошибки (4xx: 404 нет записи, 409 недостаточно товара) — пробрасываем
                if (cause instanceof FeignException feignException
                    && feignException.status() >= 400
                    && feignException.status() < 500) {
                    log.debug("Пробрасываем бизнес-ошибку от inventory-service: {}", feignException.status());
                    throw feignException;
                }

                // Технический сбой
                log.error("Деградация: сервис 'inventory-service', операция 'reserveStock (резервирование товара)', товар {}: {}",
                    request.productId(), cause.getClass().getSimpleName());
                throw new InventoryServiceUnavailableException(request.productId(), cause);
            }

            @Override
            public ReserveResponse releaseStock(ReleaseRequest request) {

                log.error("Деградация: сервис 'inventory-service', операция 'releaseStock (снятие резерва)', товар {}: {}",
                    request.productId(), cause.getClass().getSimpleName());

                return new ReserveResponse(
                    request.productId(),
                    null,  // reservedQuantity неизвестно
                    null   // availableQuantity неизвестно
                );
            }
        };
    }
}