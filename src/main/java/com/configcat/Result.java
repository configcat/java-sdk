package com.configcat;

public final class Result<T> {
    private final T value;
    private final String error;

    private Result(T value, String error) {
        this.value = value;
        this.error = error;
    }

    public T value() {
        return this.value;
    }

    public String error() {
        return this.error;
    }

    public static <T> Result<T> error(String error, T value) {
        return new Result<>(value, error);
    }

    public static <T> Result<T> success(T value) {
        return new Result<>(value, null);
    }
}
