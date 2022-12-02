package com.configcat;

final class Result<T> {
    //TODO place Result somewhere else
    private final T value;
    private final String error;

    private Result(T value, String error) {
        this.value = value;
        this.error = error;
    }

    T value() {
        return this.value;
    }

    String error() {
        return this.error;
    }

    static <T> Result<T> error(String error) {
        return new Result<>(null, error);
    }

    static <T> Result<T> success(T value) {
        return new Result<>(value, null);
    }
}
