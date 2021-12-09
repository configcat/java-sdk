package com.configcat;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

interface RefreshPolicy extends Closeable {
    CompletableFuture<Config> getConfigurationAsync();

    CompletableFuture<Void> refreshAsync();
}
