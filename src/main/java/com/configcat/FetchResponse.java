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
    private final boolean fetchTimeUpdatable;

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

    public String error() {
        if(error !=  null) {
            return error.toString();
        }
        return null;
    }

    FetchResponse(Status status, Entry entry, Object error, boolean fetchTimeUpdatable) {
        this.status = status;
        this.entry = entry;
        this.error = error;
        this.fetchTimeUpdatable = fetchTimeUpdatable;
    }

    public static FetchResponse fetched(Entry entry) {
        return new FetchResponse(Status.FETCHED, entry == null ? Entry.EMPTY : entry, null, false);
    }

    public static FetchResponse notModified() {
        return new FetchResponse(Status.NOT_MODIFIED, Entry.EMPTY, null, true);
    }

    public static FetchResponse failed(Object error, boolean fetchTimeUpdatable) {
        return new FetchResponse(Status.FAILED, Entry.EMPTY, error, fetchTimeUpdatable);
    }
}
