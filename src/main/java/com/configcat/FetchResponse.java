package com.configcat;

class FetchResponse {
    public enum Status {
        FETCHED,
        NOT_MODIFIED,
        FAILED
    }

    private final Status status;
    private final Entry entry;
    private final String error;


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

    public String error() {
        return this.error;
    }

    FetchResponse(Status status, Entry entry, String error) {
        this.status = status;
        this.entry = entry;
        this.error = error;
    }

    public static FetchResponse fetched(Entry entry) {
        return new FetchResponse(Status.FETCHED, entry == null ? Entry.empty : entry, null);
    }

    public static FetchResponse notModified() {
        return new FetchResponse(Status.NOT_MODIFIED, Entry.empty, null);
    }

    public static FetchResponse failed(String error) {
        return new FetchResponse(Status.FAILED, Entry.empty, error);
    }
}
