package com.configcat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

final class Utils {
    private Utils() { /* prevent from instantiation*/ }

    static final Gson gson = new GsonBuilder().create();

    public static Config deserializeConfig(String json){
        Config config = Utils.gson.fromJson(json, Config.class);
        String salt = config.getPreferences().getSalt();
        if(salt == null || salt.isEmpty()){
            throw new IllegalArgumentException("Config JSON salt is missing.");
        }
        for (Setting setting: config.getEntries().values()) {
            setting.setConfigSalt(config.getPreferences().getSalt());
            setting.setSegments(config.getSegments());
        }
        return config;
    }
}
