package com.configcat;

class FetchResponse {
    public enum Status {
        FETCHED,
        NOT_MODIFIED,
        FAILED
    }

    private final Status status;
    private final Entry entry;

    public boolean isFetched() {
        return this.status == Status.FETCHED;
    }

    public boolean isNotModified() {
        return this.status == Status.NOT_MODIFIED;
    }

    public boolean isFailed() {
        return this.status == Status.FAILED;
    }

    public Entry entry() {
        return this.entry;
    }

    FetchResponse(Status status, Entry entry) {
        this.status = status;
        this.entry = entry;
    }

    public static FetchResponse fetched(Entry entry) {
        return new FetchResponse(Status.FETCHED, entry == null ? Entry.empty : entry);
    }

    public static FetchResponse notModified() {
        return new FetchResponse(Status.NOT_MODIFIED, Entry.empty);
    }

    public static FetchResponse failed() {
        return new FetchResponse(Status.FAILED, Entry.empty);
    }
}
