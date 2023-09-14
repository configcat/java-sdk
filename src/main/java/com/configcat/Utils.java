package com.configcat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

final class Utils {
    private Utils() { /* prevent from instantiation*/ }

    static final Gson gson = new GsonBuilder().create();

    public static Config deserializeConfig(String json){
        //TODO clarify the salt is required and always presented or should I handle when it missing?
        Config config = Utils.gson.fromJson(json, Config.class);
        for (Setting setting: config.getEntries().values()) {
            setting.setConfigSalt(config.getPreferences().getSalt());
            setting.setSegments(config.getSegments());
        }
        return config;
    }
}
