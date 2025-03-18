package com.teamx.config;

import java.util.HashMap;
import java.util.Map;

public class DiskConfig {
    private String driver;
    private Map<String, String> options = new HashMap<>();

    public DiskConfig(String driver) {
        this.driver = driver;
    }

    public String getDriver() {
        return driver;
    }

    public DiskConfig setDriver(String driver) {
        this.driver = driver;
        return this;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public DiskConfig setOptions(Map<String, String> options) {
        this.options = options;
        return this;
    }

    public DiskConfig addOption(String key, String value) {
        options.put(key, value);
        return this;
    }

    public String getOption(String key) {
        return options.get(key);
    }

    public String getOption(String key, String defaultValue) {
        return options.getOrDefault(key, defaultValue);
    }
}
