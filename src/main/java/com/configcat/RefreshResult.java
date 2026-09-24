package com.configcat;

/**
 * Represents the result of a forceRefresh() call.
 */
public class RefreshResult {
    private final boolean success;
    private final Object error;
    private final RefreshErrorCode errorCode;
    private final Throwable errorException;

    RefreshResult(boolean success, Object error, RefreshErrorCode errorCode, Throwable errorException) {
        this.success = success;
        this.error = error;
        this.errorCode = errorCode;
        this.errorException = errorException;
    }

    public boolean isSuccess() {
        return success;
    }

    /**
     * Error message in case the operation failed, otherwise null.
     */
    public String error() {
        if (error != null) {
            return error.toString();
        }
        return null;
    }

    /**
     * The code identifying the reason for the error in case the operation failed.
     */
    public RefreshErrorCode errorCode() {
        return errorCode;
    }

    /**
     * The exception object related to the error in case the operation failed, if any.
     */
    public Throwable errorException() {
        return errorException;
    }
}