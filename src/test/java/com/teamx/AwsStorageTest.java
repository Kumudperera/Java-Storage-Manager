package com.teamx;

import com.teamx.config.DiskConfig;
import com.teamx.config.StorageConfig;
import com.teamx.disks.Storage;


public class AwsStorageTest {

    private final Storage storage;

    public AwsStorageTest() {
        StorageConfig storageConfig = new StorageConfig();
        DiskConfig diskConfig = new DiskConfig(StorageDisk.AWS_S3.value());
        diskConfig.addOption("key", "");
        diskConfig.addOption("secret", "");
        diskConfig.addOption("region", "");
        diskConfig.addOption("bucket", "");
        diskConfig.addOption("prefix", "");
        diskConfig.addOption("url", "");

        storageConfig.addDisk(StorageDisk.AWS_S3.value(), diskConfig);
        this.storage = new StorageManager(storageConfig).disk(StorageDisk.AWS_S3.value());
    }
}
