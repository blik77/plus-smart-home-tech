package ru.yandex.practicum.collector.dto.sensor;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class MotionSensorEventDto extends SensorEventDto {
    private Integer linkQuality;
    private Boolean motion;
    private Integer voltage;

    @Override
    public String getType() {
        return "MOTION_SENSOR_EVENT";
    }
}