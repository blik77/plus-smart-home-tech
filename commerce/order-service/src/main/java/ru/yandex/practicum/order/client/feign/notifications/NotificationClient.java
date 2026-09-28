package ru.yandex.practicum.order.client.feign.notifications;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.yandex.practicum.order.client.feign.dto.NotificationDto;
import ru.yandex.practicum.order.client.feign.dto.NotificationRequest;

import java.util.List;

@FeignClient(name = "notification-service")
public interface NotificationClient {

    // TODO: укажите HTTP-метод и путь
    @PostMapping("/api/notifications")
    void send(@RequestBody NotificationRequest request);

    // TODO: укажите HTTP-метод, путь и связь orderId с переменной пути
    @GetMapping("/api/notifications/order/{orderId}")
    List<NotificationDto> findByOrderId(@PathVariable Long orderId);

}
