package com.configcat;

final class Result<T> {
    private final T value;
    private final Object error;

    private Result(T value, Object error) {
        this.value = value;
        this.error = error;
    }

    T value() {
        return this.value;
    }

    Object error() {
        return this.error;
    }

    static <T> Result<T> error(Object error, T value) {
        return new Result<>(value, error);
    }

    static <T> Result<T> success(T value) {
        return new Result<>(value, null);
    }
}
