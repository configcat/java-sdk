package com.configcat;

import java.util.List;
import java.util.Map;

class EvaluationContext {
    public EvaluationContext(String key, User user, List<String> visitedKeys, Map<String, Setting> settings) {
        this.key = key;
        this.user = user;
        this.visitedKeys = visitedKeys;
        this.settings = settings;
    }

    private String key;
    private User user;
    private List<String> visitedKeys;
    private Map<String, Setting> settings;
    private boolean isUserMissing = false;
    private boolean isUserAttributeMissing = false;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setUserMissing(boolean userMissing) {
        isUserMissing = userMissing;
    }

    public void setUserAttributeMissing(boolean userAttributeMissing) {
        isUserAttributeMissing = userAttributeMissing;
    }

    public List<String> getVisitedKeys() {
        return visitedKeys;
    }

    public Map<String, Setting> getSettings() {
        return settings;
    }

    public boolean isUserMissing() {
        return isUserMissing;
    }

    public boolean isUserAttributeMissing() {
        return isUserAttributeMissing;
    }
}
