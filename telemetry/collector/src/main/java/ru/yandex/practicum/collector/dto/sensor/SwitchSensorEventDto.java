package ru.yandex.practicum.collector.dto.sensor;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class SwitchSensorEventDto extends SensorEventDto {
    private Boolean state;

    @Override
    public String getType() {
        return "SWITCH_SENSOR_EVENT";
    }
}