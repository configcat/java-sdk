package com.configcat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ConfigCatHooks {
    private final Object sync = new Object();
    private final List<Consumer<Map<String, Setting>>> onConfigChanged = new ArrayList<>();
    private final List<Runnable> onClientReady = new ArrayList<>();
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
    public void addOnClientReady(Runnable callback) {
        synchronized (sync) {
            this.onClientReady.add(callback);
        }
    }

    /**
     * Subscribes to the onConfigChanged event. This event is fired when the SDK loads a valid config.json
     * into memory from cache, and each subsequent time when the loaded config.json changes via HTTP.
     *
     * @param callback the method to call when the event fires.
     */
    public void addOnConfigChanged(Consumer<Map<String, Setting>> callback) {
        synchronized (sync) {
            this.onConfigChanged.add(callback);
        }
    }

    /**
     * Subscribes to the onError event. This event is fired when an error occurs within the ConfigCat SDK.
     *
     * @param callback the method to call when the event fires.
     */
    public void addOnError(Consumer<String> callback) {
        synchronized (sync) {
            this.onError.add(callback);
        }
    }

    /**
     * Subscribes to the onFlagEvaluated event. This event is fired each time when the SDK evaluates a feature flag or setting.
     * The event sends the same evaluation details that you would get from getValueDetails().
     *
     * @param callback the method to call when the event fires.
     */
    public void addOnFlagEvaluated(Consumer<EvaluationDetails<Object>> callback) {
        synchronized (sync) {
            this.onFlagEvaluated.add(callback);
        }
    }

    void invokeOnClientReady() {
        synchronized (sync) {
            for (Runnable func : this.onClientReady) {
                func.run();
            }
        }
    }

    void invokeOnError(String error) {
        synchronized (sync) {
            for (Consumer<String> func : this.onError) {
                func.accept(error);
            }
        }
    }

    void invokeOnConfigChanged(Map<String, Setting> settingMap) {
        synchronized (sync) {
            for (Consumer<Map<String, Setting>> func : this.onConfigChanged) {
                func.accept(settingMap);
            }
        }
    }

    void invokeOnFlagEvaluated(EvaluationDetails<Object> evaluationDetails) {
        synchronized (sync) {
            for (Consumer<EvaluationDetails<Object>> func : this.onFlagEvaluated) {
                func.accept(evaluationDetails);
            }
        }
    }

    void clear() {
        synchronized (sync) {
            this.onConfigChanged.clear();
            this.onError.clear();
            this.onFlagEvaluated.clear();
            this.onClientReady.clear();
        }
    }
}
