package com.teamx;

import com.teamx.config.DiskConfig;
import com.teamx.config.StorageConfig;
import com.teamx.disks.Storage;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

public class LocalStorageTest {

    private String root = System.getProperty("user.dir").concat("/src/test/resources/");

    private final Storage storage;

    public LocalStorageTest() {
        StorageConfig storageConfig = new StorageConfig();
        DiskConfig diskConfig = new DiskConfig(StorageDisk.LOCAL.value());
        diskConfig.addOption("root", this.root);

        storageConfig.addDisk(StorageDisk.LOCAL.value(), diskConfig);
        this.storage = new StorageManager(storageConfig).disk(StorageDisk.LOCAL.value());
    }

    @Test
    public void get() {
        InputStream inputStream = this.storage.get("test.txt");
        System.out.println("InputStream: " + inputStream);
    }

    @Test
    public void exists() {
        boolean exist = this.storage.exists("test.txt");
        System.out.println("Exist: " + exist);
    }

    @Test
    public void put() throws FileNotFoundException {
        FileInputStream file = new FileInputStream(this.root.concat("test.txt"));

        boolean filePutted = this.storage.put("put-file-test.txt", file);
        System.out.println("File putted: " + filePutted);
    }

    @Test
    public void delete() {
        boolean fileDeleted = this.storage.delete("put-file-test.txt");
        System.out.println("File Deleted: " + fileDeleted);
    }

    @Test
    public void copy() {
        boolean fileCopied = this.storage.copy("test.txt", "copy-test.txt");
        System.out.println("File Copied: " + fileCopied);
    }

    @Test
    public void makeDirectory() {
        boolean dirCreated = this.storage.makeDirectory("move");
        System.out.println("Dir Created: " + dirCreated);
    }

    @Test
    public void move() {
        boolean moved = this.storage.move("copy-test.txt", "move/copy-test.txt");
        System.out.println("File Moved: " + moved);
    }

    @Test
    public void listContents() {
        List<String> listContents = this.storage.listContents("");
        System.out.println("List Content: " + listContents);
    }

    @Test
    public void deleteDirectory() {
        boolean dirDeleted = this.storage.deleteDirectory("move");
        System.out.println("Dir Deleted: " + dirDeleted);
    }

    @Test
    public void lastModified() {
        long lastModified = this.storage.lastModified("test.txt");
        System.out.println("Last Modified: " + new Date(lastModified));
    }

    @Test
    public void size() {
        String filename = "test.txt";
        long size = this.storage.size("test.txt");
        System.out.println(filename + " File size: " + size);
    }
}
