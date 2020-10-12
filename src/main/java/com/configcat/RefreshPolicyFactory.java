package com.configcat;

class RefreshPolicyFactory implements PollingModeVisitor {
    private final ConfigCache cache;
    private final ConfigFetcher configFetcher;
    private final String apiKey;

    RefreshPolicyFactory(ConfigCache cache, ConfigFetcher configFetcher, String apiKey) {
        this.cache = cache;
        this.configFetcher = configFetcher;
        this.apiKey = apiKey;
    }

    @Override
    public RefreshPolicy visit(AutoPollingMode pollingMode) {
        return new AutoPollingPolicy(this.configFetcher, this.cache, this.apiKey, pollingMode);
    }

    @Override
    public RefreshPolicy visit(LazyLoadingMode pollingMode) {
        return new LazyLoadingPolicy(this.configFetcher, this.cache, this.apiKey, pollingMode);
    }

    @Override
    public RefreshPolicy visit(ManualPollingMode pollingMode) {
        return new ManualPollingPolicy(this.configFetcher, this.cache, this.apiKey);
    }
}
