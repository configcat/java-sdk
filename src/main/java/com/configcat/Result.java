package com.configcat;

interface ErrorCode {
    int code();
}

final class Result<T, E extends ErrorCode> {
    private final T value;
    private final Object error;
    private final E errorCode;
    private final Throwable errorException;

    private Result(T value, Object error, E errorCode, Throwable errorException) {
        this.value = value;
        this.error = error;
        this.errorCode = errorCode;
        this.errorException = errorException;
    }

    T value() {
        return this.value;
    }

    Object error() {
        return this.error;
    }

    E errorCode() {
        return this.errorCode;
    }

    Throwable errorException() {
        return this.errorException;
    }

    static <T, E extends ErrorCode> Result<T, E> error(Object error, T value, E errorCode, Throwable errorException) {
        return new Result<>(value, error, errorCode, errorException);
    }

    static <T, E extends ErrorCode> Result<T, E> success(T value, E errorCode) {
        return new Result<>(value, null, errorCode, null);
    }
}

final class EvaluationException extends IllegalArgumentException {
    EvaluationException(String message) {
        super(message);
    }
}
