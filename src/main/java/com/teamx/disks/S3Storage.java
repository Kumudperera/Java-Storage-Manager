package com.teamx.disks;

import com.teamx.StorageException;
import com.teamx.config.DiskConfig;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of Storage interface for Amazon S3
 */
public class S3Storage implements Storage {
    private final S3Client s3Client;
    private final String bucket;
    private final String baseUrl;
    private final String prefix;

    /**
     * Create an S3Storage instance with configuration
     *
     * @param config Disk configuration
     */
    public S3Storage(DiskConfig config) {
        String accessKey = config.getOption("key");
        String secretKey = config.getOption("secret");
        String regionName = config.getOption("region", "us-east-1");
        this.bucket = config.getOption("bucket");
        this.prefix = config.getOption("prefix", "");
        String url = config.getOption("url", "");

        if (bucket == null || bucket.isEmpty()) {
            throw new IllegalArgumentException("S3 bucket is required");
        }

        // Build the S3 client
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(regionName));

        if (accessKey != null && secretKey != null) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
        }

        this.s3Client = builder.build();

        // Verify bucket exists
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException e) {
            throw new IllegalArgumentException("S3 bucket does not exist: " + bucket);
        }

        this.baseUrl = url.isEmpty()
                ? "https://" + bucket + ".s3." + regionName + ".amazonaws.com/"
                : (url.endsWith("/") ? url : url + "/");
    }

    /**
     * Create an S3Storage instance with client and bucket
     *
     * @param s3Client AWS S3 client
     * @param bucket   S3 bucket name
     */
    public S3Storage(S3Client s3Client, String bucket) {
        this(s3Client, bucket, "", "");
    }

    /**
     * Create an S3Storage instance with client, bucket, and URL
     *
     * @param s3Client AWS S3 client
     * @param bucket   S3 bucket name
     * @param prefix   Prefix for all paths (like a folder)
     * @param baseUrl  Base URL for files
     */
    public S3Storage(S3Client s3Client, String bucket, String prefix, String baseUrl) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.prefix = prefix.endsWith("/") ? prefix : (prefix.isEmpty() ? "" : prefix + "/");
        this.baseUrl = baseUrl.isEmpty()
                ? "https://" + bucket + ".s3.amazonaws.com/"
                : (baseUrl.endsWith("/") ? baseUrl : baseUrl + "/");
    }

    @Override
    public InputStream get(String path) throws StorageException {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(prefixPath(path))
                    .build();

            ResponseInputStream<GetObjectResponse> objectContent = s3Client.getObject(request);
            return objectContent;
        } catch (NoSuchKeyException e) {
            throw new StorageException("File not found: " + path);
        } catch (S3Exception e) {
            throw new StorageException("Could not retrieve file from S3: " + path, e);
        }
    }

    @Override
    public boolean put(String path, InputStream content, Map<String, String> metadata) throws StorageException {
        try {
            // Build request with metadata if provided
            PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(prefixPath(path));

            if (metadata != null && !metadata.isEmpty()) {
                requestBuilder.metadata(metadata);
            }

            // Convert InputStream to bytes for RequestBody
            byte[] bytes = content.readAllBytes();

            s3Client.putObject(requestBuilder.build(), RequestBody.fromBytes(bytes));
            return true;
        } catch (S3Exception | IOException e) {
            throw new StorageException("Could not upload file to S3: " + path, e);
        }
    }

    @Override
    public boolean exists(String path) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(prefixPath(path))
                    .build();

            s3Client.headObject(request);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            throw new StorageException("Could not check if file exists on S3: " + path, e);
        }
    }

    @Override
    public boolean delete(String path) throws StorageException {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(prefixPath(path))
                    .build();

            s3Client.deleteObject(request);
            return true;
        } catch (S3Exception e) {
            throw new StorageException("Could not delete file from S3: " + path, e);
        }
    }

    @Override
    public String url(String path) {
        return baseUrl + prefixPath(path);
    }

    @Override
    public List<String> listContents(String directory) {
        try {
            String prefix = prefixPath(directory);
            if (!prefix.endsWith("/") && !prefix.isEmpty()) {
                prefix += "/";
            }

            ListObjectsV2Response response = s3Client.listObjectsV2(
                    ListObjectsV2Request.builder()
                            .bucket(bucket)
                            .prefix(prefix)
                            .build()
            );

            return response.contents().stream()
                    .map(S3Object::key)
                    .map(this::removePrefixFromPath)
                    .collect(Collectors.toList());
        } catch (S3Exception e) {
            throw new StorageException("Could not list objects in S3: " + directory, e);
        }
    }

    @Override
    public long size(String path) throws StorageException {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(prefixPath(path))
                    .build();

            HeadObjectResponse response = s3Client.headObject(request);
            return response.contentLength();
        } catch (NoSuchKeyException e) {
            throw new StorageException("File not found: " + path);
        } catch (S3Exception e) {
            throw new StorageException("Could not get file size from S3: " + path, e);
        }
    }

    @Override
    public boolean makeDirectory(String path) {
        // S3 doesn't have directories, but we can create an empty marker object
        String dirPath = path.endsWith("/") ? path : path + "/";

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(prefixPath(dirPath))
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(new byte[0]));
            return true;
        } catch (S3Exception e) {
            throw new StorageException("Could not create directory marker in S3: " + path, e);
        }
    }


    @Override
    public boolean deleteDirectory(String path) {
        try {
            String prefix = prefixPath(path);
            if (!prefix.endsWith("/") && !prefix.isEmpty()) {
                prefix += "/";
            }

            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(prefix)
                    .build();

            ListObjectsV2Response listResponse;
            do {
                listResponse = s3Client.listObjectsV2(listRequest);

                if (!listResponse.contents().isEmpty()) {
                    List<ObjectIdentifier> objectIds = listResponse.contents().stream()
                            .map(object -> ObjectIdentifier.builder().key(object.key()).build())
                            .collect(Collectors.toList());

                    // Delete objects in batches of 1000 (S3 limit)
                    for (int i = 0; i < objectIds.size(); i += 1000) {
                        int end = Math.min(i + 1000, objectIds.size());
                        List<ObjectIdentifier> batch = objectIds.subList(i, end);

                        DeleteObjectsRequest deleteRequest = DeleteObjectsRequest.builder()
                                .bucket(bucket)
                                .delete(Delete.builder().objects(batch).build())
                                .build();

                        s3Client.deleteObjects(deleteRequest);
                    }
                }

                // Continue if there are more objects
                String continuationToken = listResponse.nextContinuationToken();
                if (continuationToken != null) {
                    listRequest = listRequest.toBuilder()
                            .continuationToken(continuationToken)
                            .build();
                }

            } while (listResponse.isTruncated());

            return true;
        } catch (S3Exception e) {
            throw new StorageException("Could not delete directory from S3: " + path, e);
        }
    }

    @Override
    public long lastModified(String path) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(prefixPath(path))
                    .build();

            HeadObjectResponse response = s3Client.headObject(request);
            Instant lastModified = response.lastModified();
            return lastModified.toEpochMilli();
        } catch (NoSuchKeyException e) {
            throw new StorageException("File not found: " + path);
        } catch (S3Exception e) {
            throw new StorageException("Could not get last modified time from S3: " + path, e);
        }
    }

    @Override
    public boolean copy(String source, String destination) {
        try {
            CopyObjectRequest request = CopyObjectRequest.builder()
                    .sourceBucket(bucket)
                    .sourceKey(prefixPath(source))
                    .destinationBucket(bucket)
                    .destinationKey(prefixPath(destination))
                    .build();

            s3Client.copyObject(request);
            return true;
        } catch (NoSuchKeyException e) {
            throw new StorageException("Source file not found: " + source);
        } catch (S3Exception e) {
            throw new StorageException("Could not copy file in S3: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean move(String source, String destination) {
        try {
            // Copy then delete
            copy(source, destination);
            delete(source);
            return true;
        } catch (StorageException e) {
            throw new StorageException("Could not move file in S3: " + e.getMessage(), e);
        }
    }

    /**
     * Add prefix to the path if configured
     *
     * @param path Path without prefix
     * @return Path with prefix
     */
    private String prefixPath(String path) {
        if (prefix.isEmpty()) {
            return path;
        }

        // Remove any leading slash from the path
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;

        return prefix + normalizedPath;
    }

    /**
     * Remove prefix from path
     *
     * @param path Path with prefix
     * @return Path without prefix
     */
    private String removePrefixFromPath(String path) {
        if (prefix.isEmpty()) {
            return path;
        }

        if (path.startsWith(prefix)) {
            return path.substring(prefix.length());
        }

        return path;
    }
}
