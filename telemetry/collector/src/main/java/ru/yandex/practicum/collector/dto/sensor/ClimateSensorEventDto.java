package ru.yandex.practicum.collector.dto.sensor;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class ClimateSensorEventDto extends SensorEventDto {
    private Integer temperatureC;
    private Integer humidity;
    private Integer co2Level;

    @Override
    public String getType() {
        return "CLIMATE_SENSOR_EVENT";
    }
}