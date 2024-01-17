package com.configcat;

import java.util.HashMap;
import java.util.Map;

class LocalMapDataSource extends OverrideDataSource {
    private final Map<String, Setting> loadedSettings = new HashMap<>();

    public LocalMapDataSource(Map<String, Object> source) {
        if (source == null)
            throw new IllegalArgumentException("'source' cannot be null.");

        for (Map.Entry<String, Object> entry : source.entrySet()) {
            Setting setting = convertToSetting(entry.getValue());
            this.loadedSettings.put(entry.getKey(), setting);
        }
    }

    @Override
    public Map<String, Setting> getLocalConfiguration() {
        return this.loadedSettings;
    }

    private Setting convertToSetting(Object object) {
        Setting setting = new Setting();
        SettingsValue settingsValue = new SettingsValue();
        if (object instanceof String) {
            setting.setType(SettingType.STRING);
            settingsValue.setStringValue((String) object);
        } else if (object instanceof Boolean) {
            setting.setType(SettingType.BOOLEAN);
            settingsValue.setBooleanValue((Boolean) object);
        } else if (object instanceof Integer) {
            setting.setType(SettingType.INT);
            settingsValue.setIntegerValue((Integer) object);
        } else if (object instanceof Double) {
            setting.setType(SettingType.DOUBLE);
            settingsValue.setDoubleValue((Double) object);
        } else {
            throw new IllegalArgumentException("Only String, Integer, Double or Boolean types are supported.");
        }
        setting.setSettingsValue(settingsValue);
        return setting;
    }
}
