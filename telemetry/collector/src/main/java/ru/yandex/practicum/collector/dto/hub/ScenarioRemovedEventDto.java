package ru.yandex.practicum.collector.dto.hub;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class ScenarioRemovedEventDto extends HubEventDto {
    private String name;

    @Override
    public String getType() {
        return "SCENARIO_REMOVED";
    }
}