package ru.yandex.practicum.collector.dto.hub;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class DeviceAddedEventDto extends HubEventDto {
    private String id;
    private String deviceType;

    @Override
    public String getType() {
        return "DEVICE_ADDED";
    }
}