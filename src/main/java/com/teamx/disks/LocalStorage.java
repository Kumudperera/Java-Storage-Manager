package com.teamx.disks;

import com.teamx.Storage;
import com.teamx.StorageException;
import com.teamx.config.DiskConfig;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of Storage interface for local filesystem
 */
public class LocalStorage implements Storage {
    private final Path basePath;
    private final String baseUrl;

    /**
     * Create a LocalStorage instance with configuration
     * @param config Disk configuration
     */
    public LocalStorage(DiskConfig config) {
        String root = config.getOption("root", System.getProperty("java.io.tmpdir"));
        String url = config.getOption("url", "");

        this.basePath = Paths.get(root).toAbsolutePath().normalize();
        this.baseUrl = url.endsWith("/") ? url : url + "/";

        // Ensure base directory exists
        try {
            Files.createDirectories(basePath);
        } catch (IOException e) {
            throw new StorageException("Could not create base directory: " + basePath, e);
        }
    }

    /**
     * Create a LocalStorage instance with base path
     * @param basePath Base directory for storage
     */
    public LocalStorage(Path basePath) {
        this(basePath, "");
    }

    /**
     * Create a LocalStorage instance with base path and URL
     * @param basePath Base directory for storage
     * @param baseUrl Base URL for files
     */
    public LocalStorage(Path basePath, String baseUrl) {
        this.basePath = basePath.toAbsolutePath().normalize();
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";

        // Ensure base directory exists
        try {
            Files.createDirectories(this.basePath);
        } catch (IOException e) {
            throw new StorageException("Could not create base directory: " + this.basePath, e);
        }
    }

    @Override
    public InputStream get(String path) throws StorageException {
        Path filePath = resolveFullPath(path);

        try {
            if (!Files.exists(filePath)) {
                throw new StorageException("File not found: " + path);
            }
            return Files.newInputStream(filePath);
        } catch (IOException e) {
            throw new StorageException("Could not read file: " + path, e);
        }
    }

    @Override
    public boolean put(String path, InputStream content, Map<String, String> metadata) throws StorageException {
        Path filePath = resolveFullPath(path);

        try {
            // Create parent directories if they don't exist
            Files.createDirectories(filePath.getParent());

            // Write the file
            Files.copy(content, filePath, StandardCopyOption.REPLACE_EXISTING);

            // Metadata is ignored for local storage
            return true;
        } catch (IOException e) {
            throw new StorageException("Could not write file: " + path, e);
        }
    }

    @Override
    public boolean exists(String path) {
        return Files.exists(resolveFullPath(path));
    }

    @Override
    public boolean delete(String path) throws StorageException {
        Path filePath = resolveFullPath(path);

        try {
            if (!Files.exists(filePath)) {
                return false;
            }

            Files.delete(filePath);
            return true;
        } catch (IOException e) {
            throw new StorageException("Could not delete file: " + path, e);
        }
    }

    @Override
    public String url(String path) {
        return baseUrl + path;
    }

    @Override
    public List<String> listContents(String directory) {
        Path dirPath = resolveFullPath(directory);

        try {
            if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
                return new ArrayList<>();
            }

            return Files.list(dirPath)
                    .map(p -> basePath.relativize(p).toString().replace('\\', '/'))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new StorageException("Could not list directory: " + directory, e);
        }
    }

    @Override
    public long size(String path) throws StorageException {
        Path filePath = resolveFullPath(path);

        try {
            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                throw new StorageException("Not a file: " + path);
            }

            return Files.size(filePath);
        } catch (IOException e) {
            throw new StorageException("Could not get file size: " + path, e);
        }
    }

    @Override
    public boolean makeDirectory(String path) {
        Path dirPath = resolveFullPath(path);

        try {
            Files.createDirectories(dirPath);
            return true;
        } catch (IOException e) {
            throw new StorageException("Could not create directory: " + path, e);
        }
    }

    @Override
    public boolean deleteDirectory(String path) {
        Path dirPath = resolveFullPath(path);

        try {
            if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
                return false;
            }

            Files.walk(dirPath)
                    .sorted((p1, p2) -> -p1.compareTo(p2))
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            throw new StorageException("Could not delete: " + p, e);
                        }
                    });

            return true;
        } catch (IOException e) {
            throw new StorageException("Could not delete directory: " + path, e);
        }
    }

    @Override
    public long lastModified(String path) {
        Path filePath = resolveFullPath(path);

        try {
            if (!Files.exists(filePath)) {
                throw new StorageException("File not found: " + path);
            }

            FileTime fileTime = Files.getLastModifiedTime(filePath);
            return fileTime.toMillis();
        } catch (IOException e) {
            throw new StorageException("Could not get last modified time: " + path, e);
        }
    }

    @Override
    public boolean copy(String source, String destination) {
        Path sourcePath = resolveFullPath(source);
        Path destPath = resolveFullPath(destination);

        try {
            if (!Files.exists(sourcePath)) {
                throw new StorageException("Source file not found: " + source);
            }

            // Create parent directories if needed
            Files.createDirectories(destPath.getParent());

            Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            throw new StorageException("Failed to copy file: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean move(String source, String destination) {
        Path sourcePath = resolveFullPath(source);
        Path destPath = resolveFullPath(destination);

        try {
            if (!Files.exists(sourcePath)) {
                throw new StorageException("Source file not found: " + source);
            }

            // Create parent directories if needed
            Files.createDirectories(destPath.getParent());

            Files.move(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            throw new StorageException("Failed to move file: " + e.getMessage(), e);
        }
    }

    /**
     * Resolve a path relative to the base path
     * @param path Relative path
     * @return Absolute path
     */
    private Path resolveFullPath(String path) {
        Path normalized = Paths.get(path).normalize();

        // Security check to prevent directory traversal attacks
        if (normalized.isAbsolute() || normalized.startsWith("..")) {
            throw new StorageException("Invalid path: " + path);
        }

        return basePath.resolve(normalized);
    }
}
