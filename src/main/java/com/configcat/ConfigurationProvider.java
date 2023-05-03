package com.configcat;

import com.configcat.evaluation.EvaluationDetails;
import com.configcat.hooks.ConfigCatHooks;

import java.io.Closeable;
import java.util.Collection;
import java.util.List;
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
     * @param key          the identifier of the feature flag or setting.
     * @param defaultValue in case of any failure, this value will be returned.
     * @param <T>          the type of the desired feature flag or setting.
     * @return the configuration value identified by the given key.
     */
    <T> T getValue(Class<T> classOfT, String key, T defaultValue);

    /**
     * Gets the value of a feature flag or setting as T asynchronously identified by the given {@code key}.
     *
     * @param classOfT     the class of T. Only {@link String}, {@link Integer}, {@link Double} or {@link Boolean} types are supported.
     * @param key          the identifier of the feature flag or setting.
     * @param user         the user object.
     * @param defaultValue in case of any failure, this value will be returned.
     * @param <T>          the type of the desired feature flag or setting.
     * @return the configuration value identified by the given key.
     */
    <T> T getValue(Class<T> classOfT, String key, User user, T defaultValue);

    /**
     * Gets the value of a feature flag or setting as T identified by the given {@code key}.
     *
     * @param classOfT     the class of T. Only {@link String}, {@link Integer}, {@link Double} or {@link Boolean} types are supported.
     * @param key          the identifier of the feature flag or setting.
     * @param defaultValue in case of any failure, this value will be returned.
     * @param <T>          the type of the desired feature flag or setting.
     * @return a future which computes the configuration value identified by the given key.
     */
    <T> CompletableFuture<T> getValueAsync(Class<T> classOfT, String key, T defaultValue);

    /**
     * Gets the value of a feature flag or setting as T asynchronously identified by the given {@code key}.
     *
     * @param classOfT     the class of T. Only {@link String}, {@link Integer}, {@link Double} or {@link Boolean} types are supported.
     * @param key          the identifier of the feature flag or setting.
     * @param user         the user object.
     * @param defaultValue in case of any failure, this value will be returned.
     * @param <T>          the type of the desired feature flag or setting.
     * @return a future which computes the configuration value identified by the given key.
     */
    <T> CompletableFuture<T> getValueAsync(Class<T> classOfT, String key, User user, T defaultValue);

    /**
     * Gets the value of a feature flag or setting as T identified by the given {@code key}.
     *
     * @param classOfT     the class of T. Only {@link String}, {@link Integer}, {@link Double} or {@link Boolean} types are supported.
     * @param key          the identifier of the feature flag or setting.
     * @param defaultValue in case of any failure, this value will be returned.
     * @param <T>          the type of the desired feature flag or setting.
     * @return the result of the evaluation.
     */
    <T> EvaluationDetails<T> getValueDetails(Class<T> classOfT, String key, T defaultValue);

    /**
     * Gets the value of a feature flag or setting as T identified by the given {@code key}.
     *
     * @param classOfT     the class of T. Only {@link String}, {@link Integer}, {@link Double} or {@link Boolean} types are supported.
     * @param key          the identifier of the feature flag or setting.
     * @param user         the user object.
     * @param defaultValue in case of any failure, this value will be returned.
     * @param <T>          the type of the desired feature flag or setting.
     * @return the result of the evaluation.
     */
    <T> EvaluationDetails<T> getValueDetails(Class<T> classOfT, String key, User user, T defaultValue);

    /**
     * Gets the value of a feature flag or setting as T asynchronously identified by the given {@code key}.
     *
     * @param classOfT     the class of T. Only {@link String}, {@link Integer}, {@link Double} or {@link Boolean} types are supported.
     * @param key          the identifier of the feature flag or setting.
     * @param defaultValue in case of any failure, this value will be returned.
     * @param <T>          the type of the desired feature flag or setting.
     * @return a future which computes the evaluation details.
     */
    <T> CompletableFuture<EvaluationDetails<T>> getValueDetailsAsync(Class<T> classOfT, String key, T defaultValue);

    /**
     * Gets the value of a feature flag or setting as T asynchronously identified by the given {@code key}.
     *
     * @param classOfT     the class of T. Only {@link String}, {@link Integer}, {@link Double} or {@link Boolean} types are supported.
     * @param key          the identifier of the feature flag or setting.
     * @param user         the user object.
     * @param defaultValue in case of any failure, this value will be returned.
     * @param <T>          the type of the desired feature flag or setting.
     * @return a future which computes the evaluation details.
     */
    <T> CompletableFuture<EvaluationDetails<T>> getValueDetailsAsync(Class<T> classOfT, String key, User user, T defaultValue);

    /**
     * Gets the values of all feature flags or settings synchronously.
     *
     * @param user the user object.
     * @return a collection of all values.
     */
    Map<String, Object> getAllValues(User user);

    /**
     * Gets the values of all feature flags or settings asynchronously.
     *
     * @param user the user object.
     * @return a future which computes the collection of all values.
     */
    CompletableFuture<Map<String, Object>> getAllValuesAsync(User user);

    /**
     * Gets the detailed values of all feature flags or settings synchronously.
     *
     * @param user the user object.
     * @return a collection of all the evaluation results with details
     */
    List<EvaluationDetails<Object>> getAllValueDetails(User user);

    /**
     * Gets the detailed values of all feature flags or settings asynchronously.
     *
     * @param user the user object.
     * @return a future which computes the collection of all detailed values.
     */
    CompletableFuture<List<EvaluationDetails<Object>>> getAllValueDetailsAsync(User user);

    /**
     * Gets the key of a setting and its value identified by the given Variation ID (analytics).
     *
     * @param classOfT    the class of T. Only {@link String}, {@link Integer}, {@link Double} or {@link Boolean} types are supported.
     * @param variationId the Variation ID.
     * @param <T>         the type of the desired feature flag or setting.
     * @return the key of a setting and its value.
     */
    <T> Map.Entry<String, T> getKeyAndValue(Class<T> classOfT, String variationId);

    /**
     * Gets the key of a setting and its value identified by the given Variation ID (analytics).
     *
     * @param classOfT    the class of T. Only {@link String}, {@link Integer}, {@link Double} or {@link Boolean} types are supported.
     * @param variationId the Variation ID.
     * @param <T>         the type of the desired feature flag or setting.
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
     *
     * @return the refresh result.
     */
    RefreshResult forceRefresh();

    /**
     * Initiates a force refresh asynchronously on the cached configuration.
     *
     * @return the future which executes the refresh.
     */
    CompletableFuture<RefreshResult> forceRefreshAsync();

    /**
     * Sets defaultUser value.
     * If no user specified in the following calls {getValue}, {getAllValues}, {getValueDetails}, {getAllValueDetails}
     * the default user value will be used.
     *
     * @param defaultUser The new default user.
     */
    void setDefaultUser(User defaultUser);

    /**
     * Set default user value to null.
     */
    void clearDefaultUser();

    /**
     * Get the client closed status.
     *
     * @return True if the client is closed.
     */
    boolean isClosed();

    /**
     * Set the client to online mode. HTTP calls are allowed.
     */
    void setOnline();

    /**
     * Set the client to offline mode. HTTP calls are not allowed.
     */
    void setOffline();

    /**
     * Get the client offline mode status.
     *
     * @return True if the client is in offline mode, otherwise false.
     */
    boolean isOffline();

    /**
     * Access to hooks for event subscription.
     *
     * @return the hooks object used for event subscription.
     */
    ConfigCatHooks getHooks();
}
