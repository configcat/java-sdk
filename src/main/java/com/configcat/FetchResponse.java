package com.configcat;

class FetchResponse {
    public enum Status {
        FETCHED,
        NOT_MODIFIED,
        FAILED
    }

    private final Status status;
    private final Config config;

    public boolean isFetched() {
        return this.status == Status.FETCHED;
    }

    public boolean isNotModified() {
        return this.status == Status.NOT_MODIFIED;
    }

    public boolean isFailed() {
        return this.status == Status.FAILED;
    }

    public Config config() {
        return this.config;
    }

    FetchResponse(Status status, Config config) {
        this.status = status;
        this.config = config;
    }

    public static FetchResponse fetched(Config config) {
        return new FetchResponse(Status.FETCHED, config == null ? Config.empty : config);
    }

    public static FetchResponse notModified() {
        return new FetchResponse(Status.NOT_MODIFIED, Config.empty);
    }

    public static FetchResponse failed() {
        return new FetchResponse(Status.FAILED, Config.empty);
    }
}
