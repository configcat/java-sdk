package com.configcat;

import java.io.Closeable;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Defines the public interface of the {@link ConfigCatClient}.
 */
public interface ConfigurationProvider extends Closeable {
    /**
     * Gets the value of a feature flag or setting as T identified by the given {@code key}.
     *
     * @param classOfT     the class of T. Only {@link String}, {@link Integer}, {@link Double} or {@link Boolean} types are supported.
     * @param key          the identifier of the configuration value.
     * @param defaultValue in case of any failure, this value will be returned.
     * @param <T>          the type of the desired config value.
     * @return the configuration value identified by the given key.
     */
    <T> T getValue(Class<T> classOfT, String key, T defaultValue);

    /**
     * Gets the value of a feature flag or setting as T asynchronously identified by the given {@code key}.
     *
     * @param classOfT     the class of T. Only {@link String}, {@link Integer}, {@link Double} or {@link Boolean} types are supported.
     * @param key          the identifier of the configuration value.
     * @param user         the user object to identify the caller.
     * @param defaultValue in case of any failure, this value will be returned.
     * @param <T>          the type of the desired config value.
     * @return the configuration value identified by the given key.
     */
    <T> T getValue(Class<T> classOfT, String key, User user, T defaultValue);

    /**
     * Gets the value of a feature flag or setting as T identified by the given {@code key}.
     *
     * @param classOfT     the class of T. Only {@link String}, {@link Integer}, {@link Double} or {@link Boolean} types are supported.
     * @param key          the identifier of the configuration value.
     * @param defaultValue in case of any failure, this value will be returned.
     * @param <T>          the type of the desired config value.
     * @return a future which computes the configuration value identified by the given key.
     */
    <T> CompletableFuture<T> getValueAsync(Class<T> classOfT, String key, T defaultValue);

    /**
     * Gets the value of a feature flag or setting as T asynchronously identified by the given {@code key}.
     *
     * @param classOfT     the class of T. Only {@link String}, {@link Integer}, {@link Double} or {@link Boolean} types are supported.
     * @param key          the identifier of the configuration value.
     * @param user         the user object to identify the caller.
     * @param defaultValue in case of any failure, this value will be returned.
     * @param <T>          the type of the desired config value.
     * @return a future which computes the configuration value identified by the given key.
     */
    <T> CompletableFuture<T> getValueAsync(Class<T> classOfT, String key, User user, T defaultValue);

    /**
     * Gets the Variation ID (analytics) of a feature flag or setting synchronously based on its key.
     *
     * @param key                the identifier of the configuration value.
     * @param defaultVariationId in case of any failure, this value will be returned.
     * @return the Variation ID.
     */
    String getVariationId(String key, String defaultVariationId);

    /**
     * Gets the Variation ID (analytics) of a feature flag or setting synchronously based on its key.
     *
     * @param key                the identifier of the configuration value.
     * @param user               the user object to identify the caller.
     * @param defaultVariationId in case of any failure, this value will be returned.
     * @return the Variation ID.
     */
    String getVariationId(String key, User user, String defaultVariationId);

    /**
     * Gets the Variation ID (analytics) of a feature flag or setting asynchronously based on its key.
     *
     * @param key                the identifier of the configuration value.
     * @param defaultVariationId in case of any failure, this value will be returned.
     * @return a future which computes the Variation ID.
     */
    CompletableFuture<String> getVariationIdAsync(String key, String defaultVariationId);

    /**
     * Gets the Variation ID (analytics) of a feature flag or setting asynchronously based on its key.
     *
     * @param key                the identifier of the configuration value.
     * @param user               the user object to identify the caller.
     * @param defaultVariationId in case of any failure, this value will be returned.
     * @return a future which computes the Variation ID.
     */
    CompletableFuture<String> getVariationIdAsync(String key, User user, String defaultVariationId);

    /**
     * Gets the Variation IDs (analytics) of all feature flags or settings synchronously.
     *
     * @return a collection of all Variation IDs.
     */
    Collection<String> getAllVariationIds();

    /**
     * Gets the Variation IDs (analytics) of all feature flags or settings asynchronously.
     *
     * @return a future which computes the collection of all Variation IDs.
     */
    CompletableFuture<Collection<String>> getAllVariationIdsAsync();

    /**
     * Gets the Variation IDs (analytics) of all feature flags or settings synchronously.
     *
     * @param user the user object to identify the caller.
     * @return a collection of all Variation IDs.
     */
    Collection<String> getAllVariationIds(User user);

    /**
     * Gets the Variation IDs (analytics) of all feature flags or settings asynchronously.
     *
     * @param user the user object to identify the caller.
     * @return a future which computes the collection of all Variation IDs.
     */
    CompletableFuture<Collection<String>> getAllVariationIdsAsync(User user);

    /**
     * Gets the values of all feature flags or settings synchronously.
     *
     * @param user the user object to identify the caller.
     * @return a collection of all values.
     */
    Map<String, Object> getAllValues(User user);

    /**
     * Gets the values of all feature flags or settings asynchronously.
     *
     * @param user the user object to identify the caller.
     * @return a future which computes the collection of all values.
     */
    CompletableFuture<Map<String, Object>> getAllValuesAsync(User user);

    /**
     * Gets the key of a setting and its value identified by the given Variation ID (analytics).
     *
     * @param classOfT    the class of T. Only {@link String}, {@link Integer}, {@link Double} or {@link Boolean} types are supported.
     * @param variationId the Variation ID.
     * @param <T>         the type of the desired config value.
     * @return the key of a setting and its value.
     */
    <T> Map.Entry<String, T> getKeyAndValue(Class<T> classOfT, String variationId);

    /**
     * Gets the key of a setting and its value identified by the given Variation ID (analytics).
     *
     * @param classOfT    the class of T. Only {@link String}, {@link Integer}, {@link Double} or {@link Boolean} types are supported.
     * @param variationId the Variation ID.
     * @param <T>         the type of the desired config value.
     * @return a future which computes the key of a setting and its value.
     */
    <T> CompletableFuture<Map.Entry<String, T>> getKeyAndValueAsync(Class<T> classOfT, String variationId);

    /**
     * Gets a collection of all setting keys.
     *
     * @return a collection of all setting keys.
     */
    Collection<String> getAllKeys();

    /**
     * Gets a collection of all setting keys asynchronously.
     *
     * @return a collection of all setting keys.
     */
    CompletableFuture<Collection<String>> getAllKeysAsync();

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

    /**
     * Sets defaultUser value.
     * If no user specified in the following calls {getValue}, {getAllValues}, {getVariationId},
     * {getAllVariationIds}, {getValueAsync},  {getAllValuesAsync}, {getVariationIdAsync}, {getAllVariationIdsAsync}
     * the default user value will be used.
     *
     * @param defaultUser The new default user.
     */
    void setDefaultUser(User defaultUser);

    /**
     * Set default user value to null.
     */
    void clearDefaultUser();

}
