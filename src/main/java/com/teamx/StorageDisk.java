package com.teamx;

public enum StorageDisk {
    LOCAL("local"), AWS_S3("aws-s3");

    private final String value;

    private StorageDisk(String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }

    public static StorageDisk resolveDisk(String value) {
        StorageDisk disk = null;
        for (StorageDisk storageDisk : StorageDisk.values()) {
            if (storageDisk.value().equals(value)) {
                disk = storageDisk;
                break;
            }
        }
        return disk;
    }
}
