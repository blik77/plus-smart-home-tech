package ru.yandex.practicum.analyzer.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "scenario_conditions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScenarioCondition {

    @EmbeddedId
    @Builder.Default
    private ScenarioConditionId id = new ScenarioConditionId();

    @MapsId("scenarioId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id")
    private Scenario scenario;

    @MapsId("sensorId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_id")
    private Sensor sensor;

    @MapsId("conditionId")
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "condition_id")
    private Condition condition;
}