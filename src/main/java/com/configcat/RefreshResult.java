package com.configcat;

/**
 * Represents the result of a forceRefresh() call.
 */
public class RefreshResult {
    private final boolean success;
    private final Object error;

    RefreshResult(boolean success, Object error) {
        this.success = success;
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public String error() {
        if(error !=  null) {
            return error.toString();
        }
        return null;
    }
}