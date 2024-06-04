package com.configcat;

/**
 * Describes the Client state.
 */
public enum ClientCacheState {
    /**
     *  The SDK has no feature flag data neither from the cache nor from the ConfigCat CDN.
     */
    NO_FLAG_DATA,
    /**
     * The SDK runs with local only feature flag data.
     */
    HAS_LOCAL_OVERRIDE_FLAG_DATA_ONLY,
    /**
     * The SDK has feature flag data to work with only from the cache.
     */
    HAS_CACHED_FLAG_DATA_ONLY,
    /**
     * The SDK works with the latest feature flag data received from the ConfigCat CDN.
     */
    HAS_UP_TO_DATE_FLAG_DATA,
}
