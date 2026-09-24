package com.configcat;

class FetchResponse {
    public enum Status {
        FETCHED,
        NOT_MODIFIED,
        FAILED
    }

    private final Status status;
    private final Entry entry;
    private final Object error;
    private final RefreshErrorCode errorCode;
    private final Throwable errorException;
    private final boolean fetchTimeUpdatable;
    private final String cfRayId;
    private final boolean shouldRetry;

    public boolean isFetched() {
        return this.status == Status.FETCHED;
    }

    public boolean isNotModified() {
        return this.status == Status.NOT_MODIFIED;
    }

    public boolean isFailed() {
        return this.status == Status.FAILED;
    }

    public boolean isFetchTimeUpdatable() {
        return fetchTimeUpdatable;
    }

    public Entry entry() {
        return this.entry;
    }

    public Object error() {
       return error;
    }

    public RefreshErrorCode errorCode() {return this.errorCode;}

    public Throwable errorException() {return this.errorException;}

    public String cfRayId() {return this.cfRayId;}

    public boolean shouldRetry() {return shouldRetry;}

    FetchResponse(Status status, Entry entry, Object error, RefreshErrorCode errorCode, Throwable errorException,
                  boolean fetchTimeUpdatable, String cfRayId, boolean shouldRetry) {
        this.status = status;
        this.entry = entry;
        this.error = error;
        this.errorCode = errorCode;
        this.errorException = errorException;
        this.fetchTimeUpdatable = fetchTimeUpdatable;
        this.cfRayId = cfRayId;
        this.shouldRetry = shouldRetry;
    }

    public static FetchResponse fetched(Entry entry, String cfRayId) {
        return new FetchResponse(Status.FETCHED, entry == null ? Entry.EMPTY : entry, null,
                RefreshErrorCode.NONE, null, false, cfRayId, false);
    }

    public static FetchResponse notModified(String cfRayId) {
        return new FetchResponse(Status.NOT_MODIFIED, Entry.EMPTY, null, RefreshErrorCode.NONE, null,
                true, cfRayId, false);
    }

    public static FetchResponse failed(Object error, RefreshErrorCode errorCode, Throwable errorException,
                                       boolean fetchTimeUpdatable, String cfRayId, boolean shouldRetry) {
        return new FetchResponse(Status.FAILED, Entry.EMPTY, error, errorCode, errorException,
                fetchTimeUpdatable, cfRayId, shouldRetry);
    }
}
