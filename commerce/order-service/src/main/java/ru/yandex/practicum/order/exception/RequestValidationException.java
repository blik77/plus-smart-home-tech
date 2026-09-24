package ru.yandex.practicum.order.exception;

import java.util.Map;

public class RequestValidationException extends RuntimeException {
    private final Map<String, String> errors;

    public RequestValidationException(Map<String, String> errors) {
        super("Ошибка валидации");
        this.errors = errors;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
