package ru.yandex.practicum.collector.dto.sensor;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class LightSensorEventDto extends SensorEventDto {
    private Integer linkQuality;
    private Integer luminosity;

    @Override
    public String getType() {
        return "LIGHT_SENSOR_EVENT";
    }
}