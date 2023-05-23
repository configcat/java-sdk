package com.configcat;


import com.configcat.models.Entry;

public class FetchResponse {
    enum Status {
        FETCHED,
        NOT_MODIFIED,
        FAILED
    }

    private final Status status;
    private final Entry entry;
    private final String error;
    private final boolean fetchTimeUpdatable;

    private final String fetchTime;

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
        return this.error;
    }

    public String getFetchTime() {
        return this.fetchTime;
    }

    FetchResponse(Status status, Entry entry, String error, boolean fetchTimeUpdatable, String fetchTime) {
        this.status = status;
        this.entry = entry;
        this.error = error;
        this.fetchTimeUpdatable = fetchTimeUpdatable;
        this.fetchTime = fetchTime;
    }

    public static FetchResponse fetched(Entry entry, String fetchTime) {
        return new FetchResponse(Status.FETCHED, entry == null ? Entry.EMPTY : entry, null, false, fetchTime);
    }

    public static FetchResponse notModified(String fetchTime) {
        return new FetchResponse(Status.NOT_MODIFIED, Entry.EMPTY, null, true, fetchTime);
    }

    public static FetchResponse failed(String error, boolean fetchTimeUpdatable, String fetchTime) {
        return new FetchResponse(Status.FAILED, Entry.EMPTY, error, fetchTimeUpdatable, fetchTime);
    }
}
