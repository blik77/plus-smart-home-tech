package ru.yandex.practicum.collector.dto.hub;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString(callSuper = true)
public class ScenarioAddedEventDto extends HubEventDto {
    @Size(min = 3)
    private String name;

    @Valid
    @NotEmpty
    private List<ScenarioConditionDto> conditions;

    @Valid
    @NotEmpty
    private List<DeviceActionDto> actions;

    @Override
    public String getType() {
        return "SCENARIO_ADDED";
    }
}