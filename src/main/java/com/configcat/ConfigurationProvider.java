package com.configcat;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

/**
 * Defines the public interface of the {@link ConfigCatClient}.
 */
public interface ConfigurationProvider extends Closeable {
    /**
     * Gets the internal refresh policy.
     *
     * @param classOfT the class of T.
     * @param <T> the type of the desired policy.
     * @return the internal policy.
     */
    <T extends RefreshPolicy> T getRefreshPolicy(Class<T> classOfT);

    /**
     * Gets a value synchronously as T from the configuration identified by the given {@code key}.
     *
     * @param classOfT the class of T. Only {@link String}, {@link Integer}, {@link Double} or {@link Boolean} types are supported.
     * @param key the identifier of the configuration value.
     * @param defaultValue in case of any failure, this value will be returned.
     * @param <T> the type of the desired config value.
     * @return the configuration value identified by the given key.
     */
    <T> T getValue(Class<T> classOfT, String key, T defaultValue);

    /**
     * Gets a value synchronously as T from the configuration identified by the given {@code key}.
     *
     * @param classOfT the class of T. Only {@link String}, {@link Integer}, {@link Double} or {@link Boolean} types are supported.
     * @param key the identifier of the configuration value.
     * @param user the user object to identify the caller.
     * @param defaultValue in case of any failure, this value will be returned.
     * @param <T> the type of the desired config value.
     * @return the configuration value identified by the given key.
     */
    <T> T getValue(Class<T> classOfT, String key, User user, T defaultValue);

    /**
     * Gets a value asynchronously as T from the configuration identified by the given {@code key}.
     *
     * @param classOfT the class of T. Only {@link String}, {@link Integer}, {@link Double} or {@link Boolean} types are supported.
     * @param key the identifier of the configuration value.
     * @param defaultValue in case of any failure, this value will be returned.
     * @param <T> the type of the desired config value.
     * @return a future which computes the configuration value identified by the given key.
     */
    <T> CompletableFuture<T> getValueAsync(Class<T> classOfT, String key, T defaultValue);

    /**
     * Gets a value asynchronously as T from the configuration identified by the given {@code key}.
     *
     * @param classOfT the class of T. Only {@link String}, {@link Integer}, {@link Double} or {@link Boolean} types are supported.
     * @param key the identifier of the configuration value.
     * @param user the user object to identify the caller.
     * @param defaultValue in case of any failure, this value will be returned.
     * @param <T> the type of the desired config value.
     * @return a future which computes the configuration value identified by the given key.
     */
    <T> CompletableFuture<T> getValueAsync(Class<T> classOfT, String key, User user, T defaultValue);

    /**
     * Initiates a force refresh synchronously on the cached configuration.
     */
    void forceRefresh();

    /**
     * Initiates a force refresh asynchronously on the cached configuration.
     *
     * @return the future which executes the refresh.
     */
    CompletableFuture<Void> forceRefreshAsync();
}
