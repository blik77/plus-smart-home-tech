package ru.yandex.practicum.analyzer.service;

import com.google.protobuf.Timestamp;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.analyzer.model.ActionToExecute;
import ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc.HubRouterControllerBlockingStub;

@Slf4j
@Service
public class ActionExecutorImpl implements ActionExecutor {

    private final HubRouterControllerBlockingStub hubRouterClient;

    public ActionExecutorImpl(@GrpcClient("hub-router") HubRouterControllerBlockingStub hubRouterClient) {
        this.hubRouterClient = hubRouterClient;
    }

    @Override
    public void execute(ActionToExecute action) {
        DeviceActionRequest request = DeviceActionRequest.newBuilder()
            .setHubId(action.hubId())
            .setScenarioName(action.scenarioName())
            .setAction(toProto(action))
            .setTimestamp(toProtoTimestamp(action.timestamp()))
            .build();
        try {
            hubRouterClient.handleDeviceAction(request);
            log.info("Команда отправлена: хаб={}, сценарий='{}', датчик={}, тип={}, value={}",
                action.hubId(), action.scenarioName(), action.sensorId(),
                action.type(), action.value());
        } catch (StatusRuntimeException e) {
            log.error("Ошибка gRPC-вызова к HubRouter: {}", e.getStatus().getCode(), e);
            throw new StatusRuntimeException(e.getStatus());
        }
    }

    private DeviceActionProto toProto(ActionToExecute action) {
        return DeviceActionProto.newBuilder()
            .setSensorId(action.sensorId())
            .setType(ActionTypeProto.valueOf(action.type().name()))
            .setValue(action.value() == null ? 0 : action.value())
            .build();
    }

    private Timestamp toProtoTimestamp(java.time.Instant instant) {
        return Timestamp.newBuilder()
            .setSeconds(instant.getEpochSecond())
            .setNanos(instant.getNano())
            .build();
    }
}