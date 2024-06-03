package com.configcat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

public class ConfigCatHooks {
    private final AtomicReference<ClientReadyState> clientReadyState = new AtomicReference<>(null);
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private final List<Consumer<Map<String, Setting>>> onConfigChanged = new ArrayList<>();
    private final List<Consumer<ClientReadyState>> onClientReady = new ArrayList<>();
    private final List<Consumer<EvaluationDetails<Object>>> onFlagEvaluated = new ArrayList<>();
    private final List<Consumer<String>> onError = new ArrayList<>();

    /**
     * Subscribes to the onReady event. This event is fired when the SDK reaches the ready state.
     * If the SDK is configured with lazy load or manual polling it's considered ready right after instantiation.
     * In case of auto polling, the ready state is reached when the SDK has a valid config.json loaded
     * into memory either from cache or from HTTP. If the config couldn't be loaded neither from cache nor from HTTP the
     * onReady event fires when the auto polling's maxInitWaitTimeInSeconds is reached.
     *
     * @param callback the method to call when the event fires.
     */
    public void addOnClientReady(Consumer<ClientReadyState> callback) {
        lock.writeLock().lock();
        try {
            if(clientReadyState.get() != null) {
                callback.accept(clientReadyState.get());
            } else {
                this.onClientReady.add(callback);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Subscribes to the onConfigChanged event. This event is fired when the SDK loads a valid config.json
     * into memory from cache, and each subsequent time when the loaded config.json changes via HTTP.
     *
     * @param callback the method to call when the event fires.
     */
    public void addOnConfigChanged(Consumer<Map<String, Setting>> callback) {
        lock.writeLock().lock();
        try {
            this.onConfigChanged.add(callback);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Subscribes to the onError event. This event is fired when an error occurs within the ConfigCat SDK.
     *
     * @param callback the method to call when the event fires.
     */
    public void addOnError(Consumer<String> callback) {
        lock.writeLock().lock();
        try {
            this.onError.add(callback);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Subscribes to the onFlagEvaluated event. This event is fired each time when the SDK evaluates a feature flag or setting.
     * The event sends the same evaluation details that you would get from getValueDetails().
     *
     * @param callback the method to call when the event fires.
     */
    public void addOnFlagEvaluated(Consumer<EvaluationDetails<Object>> callback) {
        lock.writeLock().lock();
        try {
            this.onFlagEvaluated.add(callback);
        } finally {
            lock.writeLock().unlock();
        }
    }

    void invokeOnClientReady(ClientReadyState clientReadyState) {
        lock.readLock().lock();
        try {
            this.clientReadyState.set(clientReadyState);
            for (Consumer<ClientReadyState> func : this.onClientReady) {
                func.accept(clientReadyState);
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    void invokeOnError(String error) {
        lock.readLock().lock();
        try {
            for (Consumer<String> func : this.onError) {
                func.accept(error);
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    void invokeOnConfigChanged(Map<String, Setting> settingMap) {
        lock.readLock().lock();
        try {
            for (Consumer<Map<String, Setting>> func : this.onConfigChanged) {
                func.accept(settingMap);
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    void invokeOnFlagEvaluated(EvaluationDetails<Object> evaluationDetails) {
        lock.readLock().lock();
        try {
            for (Consumer<EvaluationDetails<Object>> func : this.onFlagEvaluated) {
                func.accept(evaluationDetails);
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    void clear() {
        lock.writeLock().lock();
        try {
            this.onConfigChanged.clear();
            this.onError.clear();
            this.onFlagEvaluated.clear();
            this.onClientReady.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
