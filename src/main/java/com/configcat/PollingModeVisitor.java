package com.configcat;

interface PollingModeVisitor {
    RefreshPolicy visit(AutoPollingMode pollingMode);
    RefreshPolicy visit(LazyLoadingMode pollingMode);
    RefreshPolicy visit(ManualPollingMode pollingMode);
}
