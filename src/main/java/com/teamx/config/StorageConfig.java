package com.teamx.config;

import java.util.HashMap;
import java.util.Map;

public class StorageConfig {
    private String defaultDisk = "local";
    private final Map<String, DiskConfig> disks = new HashMap<>();

    public StorageConfig() {
        // Default constructor
    }

    /**
     * Get the default disk name
     * @return Default disk name
     */
    public String getDefaultDisk() {
        return defaultDisk;
    }

    /**
     * Set the default disk name
     * @param defaultDisk Default disk name
     * @return This instance for chaining
     */
    public StorageConfig setDefaultDisk(String defaultDisk) {
        this.defaultDisk = defaultDisk;
        return this;
    }

    /**
     * Get disk configurations
     * @return Map of disk configurations
     */
    public Map<String, DiskConfig> getDisks() {
        return disks;
    }

    /**
     * Add a disk configuration
     * @param name Disk name
     * @param config Disk configuration
     * @return This instance for chaining
     */
    public StorageConfig addDisk(String name, DiskConfig config) {
        disks.put(name, config);
        return this;
    }
}
