package com.configcat;

/**
 * Describes the location of your feature flag and setting data within the ConfigCat CDN.
 */
public enum DataGovernance {
    /**
     * Your data will be published to all ConfigCat CDN nodes to guarantee lowest response times.
     */
    GLOBAL,
    /**
     * Your data will be published to CDN nodes only in the EU.
     */
    EU_ONLY
}
