package ru.yandex.practicum.collector.dto.hub;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class DeviceRemovedEventDto extends HubEventDto {
    private String id;

    @Override
    public String getType() {
        return "DEVICE_REMOVED";
    }
}