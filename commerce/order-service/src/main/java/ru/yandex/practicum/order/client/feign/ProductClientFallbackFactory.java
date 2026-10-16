package ru.yandex.practicum.order.client.feign;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.order.client.feign.dto.ProductDto;
import ru.yandex.practicum.order.exception.ProductServiceUnavailableException;

@Slf4j
@Component
public class ProductClientFallbackFactory implements FallbackFactory<ProductClient> {

    @Override
    public ProductClient create(Throwable cause) {
        log.warn("Fallback сработал для ProductClient. Причина: {}", cause.getMessage());

        return new ProductClient() {
            @Override
            public ProductDto getProductById(Long productId) {
                // Бизнес-ошибки (4xx) пробрасываем дальше — это не технический сбой
                if (cause instanceof FeignException feignException
                    && feignException.status() >= 400
                    && feignException.status() < 500) {
                    log.debug("Пробрасываем бизнес-ошибку от product-service: {}", feignException.status());
                    throw feignException;
                }

                // Технический сбой: таймаут, 5xx, Circuit Breaker открыт, сервис недоступен
                log.error("Деградация: сервис 'product-service', операция 'getProductById (получение товара из каталога)', товар {}: {}",
                    productId, cause.getClass().getSimpleName());
                throw new ProductServiceUnavailableException(productId, cause);
            }
        };
    }
}