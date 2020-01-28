package com.configcat;

/**
 * This class represents a fetch response object.
 */
class FetchResponse {
    /**
     * The response status, can be {@code FETCHED}, {@code NOTMODIFIED} or {@code FAILED}.
     */
    public enum Status {
        FETCHED,
        NOTMODIFIED,
        FAILED
    }

    private Status status;
    private String config;

    /**
     * Gets whether a new configuration value was fetched or not.
     *
     * @return true if a new configuration value was fetched, otherwise false.
     */
    public boolean isFetched() {
        return this.status == Status.FETCHED;
    }

    /**
     * Gets whether the fetch resulted a '304 Not Modified' or not.
     *
     * @return true if the fetch resulted a '304 Not Modified' code, otherwise false.
     */
    public boolean isNotModified() {
        return this.status == Status.NOTMODIFIED;
    }

    /**
     * Gets whether the fetch failed or not.
     *
     * @return true if the fetch is failed, otherwise false.
     */
    public boolean isFailed() {
        return this.status == Status.FAILED;
    }

    /**
     * Gets the fetched configuration value, should be used when the response
     * has a {@code FetchResponse.Status.FETCHED} status code.
     *
     * @return the fetched config.
     */
    public String config() {
        return this.config;
    }

    FetchResponse(Status status, String config) {
        this.status = status;
        this.config = config;
    }
}
