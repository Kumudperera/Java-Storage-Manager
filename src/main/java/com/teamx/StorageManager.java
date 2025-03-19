package com.teamx;

import com.teamx.config.StorageConfig;
import com.teamx.disks.LocalStorage;
import com.teamx.disks.S3Storage;
import com.teamx.disks.Storage;
//import com.teamx.disks.FtpStorage;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory class to manage and provide access to storage implementations
 */
public class StorageManager {
    private final Map<String, Storage> disks = new HashMap<>();
    private String defaultDisk;

    /**
     * Initialize StorageManager with default configuration
     */
    public StorageManager() {
        defaultDisk = "local";
    }

    /**
     * Initialize StorageManager with configuration
     *
     * @param config Storage configuration
     */
    public StorageManager(StorageConfig config) {
        this.defaultDisk = config.getDefaultDisk();

        // Setup configured disks
        config.getDisks().forEach((diskName, diskConfig) -> {
            String driver = diskConfig.getDriver();
            StorageDisk disk = StorageDisk.resolveDisk(driver);

            switch (disk) {
                case LOCAL:
                    disks.put(diskName, new LocalStorage(diskConfig));
                    break;
                case AWS_S3:
                    disks.put(diskName, new S3Storage(diskConfig));
                    break;
                /*case "ftp":
                    disks.put(diskName, new FtpStorage(diskConfig));
                    break;*/
                default:
                    throw new IllegalArgumentException("Unsupported driver: " + driver);
            }
        });
    }

    /**
     * Add a disk to the manager
     *
     * @param name    Disk name
     * @param storage Storage implementation
     */
    public void addDisk(String name, Storage storage) {
        disks.put(name, storage);
    }

    /**
     * Get a specific disk
     *
     * @param name Disk name
     * @return Storage implementation
     */
    public Storage disk(String name) {
        if (!disks.containsKey(name)) {
            throw new IllegalArgumentException("Disk not found: " + name);
        }
        return disks.get(name);
    }

    /**
     * Get the default disk
     *
     * @return Default storage implementation
     */
    public Storage disk() {
        return disk(defaultDisk);
    }

    /**
     * Set the default disk
     *
     * @param name Disk name
     */
    public void setDefaultDisk(String name) {
        if (!disks.containsKey(name)) {
            throw new IllegalArgumentException("Cannot set default disk. Disk not found: " + name);
        }
        this.defaultDisk = name;
    }

    /**
     * Get the current default disk name
     *
     * @return Default disk name
     */
    public String getDefaultDiskName() {
        return defaultDisk;
    }
}
