package ru.yandex.practicum.collector.dto.hub;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class ScenarioConditionDto {
    private String sensorId;
    private String type;
    private String operation;
    private Object value;
}