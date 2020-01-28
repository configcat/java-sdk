package com.configcat;

class RefreshPolicyFactory implements PollingModeVisitor {
    private final ConfigCache cache;
    private final ConfigFetcher configFetcher;

    RefreshPolicyFactory(ConfigCache cache, ConfigFetcher configFetcher) {
        this.cache = cache;
        this.configFetcher = configFetcher;
    }

    @Override
    public RefreshPolicy visit(AutoPollingMode pollingMode) {
        return new AutoPollingPolicy(this.configFetcher, this.cache, pollingMode);
    }

    @Override
    public RefreshPolicy visit(LazyLoadingMode pollingMode) {
        return new LazyLoadingPolicy(this.configFetcher, this.cache, pollingMode);
    }

    @Override
    public RefreshPolicy visit(ManualPollingMode pollingMode) {
        return new ManualPollingPolicy(this.configFetcher, this.cache);
    }
}
