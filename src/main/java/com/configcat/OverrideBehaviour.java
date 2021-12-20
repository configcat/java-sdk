package com.configcat;

/**
 * Describes how the overrides should behave.
 */
public enum OverrideBehaviour {
    /**
     * With this mode, the SDK won't fetch the flags & settings from the ConfigCat CDN, and it will use only the local
     * overrides to evaluate values.
     */
    LOCAL_ONLY,
    /**
     * With this mode, the SDK will fetch the feature flags & settings from the ConfigCat CDN, and it will replace
     * those that have a matching key in the flag overrides.
     */
    LOCAL_OVER_REMOTE,
    /**
     * With this mode, the SDK will fetch the feature flags & settings from the ConfigCat CDN, and it will use the
     * overrides for only those flags that doesn't exist in the fetched configuration.
     */
    REMOTE_OVER_LOCAL,
}
