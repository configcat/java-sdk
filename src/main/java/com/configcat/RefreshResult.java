package com.configcat;

/**
 * Represents the result of a forceRefresh() call.
 */
public class RefreshResult {
    private final boolean success;
    private final String error;

    RefreshResult(boolean success, String error) {
        this.success = success;
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public String error() {
        return error;
    }
}