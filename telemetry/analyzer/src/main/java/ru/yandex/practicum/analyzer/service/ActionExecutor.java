package ru.yandex.practicum.analyzer.service;

import ru.yandex.practicum.analyzer.model.ActionToExecute;

public interface ActionExecutor {
    void execute(ActionToExecute action);
}