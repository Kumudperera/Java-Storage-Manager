package com.teamx;

import com.teamx.config.DiskConfig;
import com.teamx.config.StorageConfig;

public class Main {
    public static void main(String[] args) {
        DiskConfig diskConfig = new DiskConfig(StorageManager.StorageDisk.AWS_S3.getValue());
        StorageConfig storageConfig = new StorageConfig();

        diskConfig.addOption("key", "");
        diskConfig.addOption("secret", "");
        diskConfig.addOption("region", "");
        diskConfig.addOption("bucket", "");
        diskConfig.addOption("prefix", "");
        diskConfig.addOption("url", "");

        storageConfig.addDisk(StorageManager.StorageDisk.AWS_S3.getValue(), diskConfig);

        StorageManager storageManager = new StorageManager(storageConfig);
    }

    private static void getDiskConfig() {
    }
}