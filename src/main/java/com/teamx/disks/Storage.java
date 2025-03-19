package com.teamx.disks;

import com.teamx.StorageException;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Main interface for all storage implementations.
 * Provides abstraction for common storage operations.
 */
public interface Storage {
    /**
     * Retrieves a file from storage as an InputStream
     *
     * @param path Path to the file
     * @return InputStream containing the file content
     * @throws StorageException if file cannot be retrieved
     */
    InputStream get(String path) throws StorageException;

    /**
     * Stores a file in the storage
     *
     * @param path     Path where to store the file
     * @param content  InputStream containing the file content
     * @param metadata Optional metadata for the file
     * @return true if successful
     * @throws StorageException if file cannot be stored
     */
    boolean put(String path, InputStream content, Map<String, String> metadata) throws StorageException;

    /**
     * Overloaded method to put file without metadata
     */
    default boolean put(String path, InputStream content) throws StorageException {
        return put(path, content, null);
    }

    /**
     * Checks if a file exists
     *
     * @param path Path to check
     * @return true if file exists
     */
    boolean exists(String path);

    /**
     * Deletes a file from storage
     *
     * @param path Path to the file
     * @return true if successfully deleted
     * @throws StorageException if file cannot be deleted
     */
    boolean delete(String path) throws StorageException;

    /**
     * Gets a URL for a file
     *
     * @param path Path to the file
     * @return URL to access the file
     */
    String url(String path);

    /**
     * Lists files in a directory
     *
     * @param directory Directory path
     * @return List of file paths
     */
    List<String> listContents(String directory);

    /**
     * Gets the size of a file in bytes
     *
     * @param path Path to the file
     * @return Size in bytes
     * @throws StorageException if size cannot be determined
     */
    long size(String path) throws StorageException;

    /**
     * Creates a directory
     *
     * @param path Directory path
     * @return true if successfully created
     */
    boolean makeDirectory(String path);

    /**
     * Deletes a directory
     *
     * @param path Directory path
     * @return true if successfully deleted
     */
    boolean deleteDirectory(String path);

    /**
     * Gets the last modified time of a file
     *
     * @param path Path to the file
     * @return Last modified time in milliseconds
     */
    long lastModified(String path);

    /**
     * Copies a file from one location to another
     *
     * @param source      Source path
     * @param destination Destination path
     * @return true if successful
     */
    boolean copy(String source, String destination);

    /**
     * Moves a file from one location to another
     *
     * @param source      Source path
     * @param destination Destination path
     * @return true if successful
     */
    boolean move(String source, String destination);
}
