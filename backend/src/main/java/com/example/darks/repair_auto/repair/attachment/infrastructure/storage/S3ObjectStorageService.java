package com.example.darks.repair_auto.repair.attachment.infrastructure.storage;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Service
@ConditionalOnProperty(prefix = "app.storage", name = "enabled", havingValue = "true")
public class S3ObjectStorageService implements ObjectStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3ObjectStorageService.class);

    private final StorageProperties properties;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public S3ObjectStorageService(StorageProperties properties) {
        this.properties = properties;
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.accessKey(), properties.secretKey()));
        S3Configuration s3Configuration = S3Configuration.builder()
                .pathStyleAccessEnabled(properties.pathStyle())
                .chunkedEncodingEnabled(false)
                .checksumValidationEnabled(false)
                .build();
        S3ClientBuilder clientBuilder = S3Client.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(credentialsProvider)
                .serviceConfiguration(s3Configuration);
        S3Presigner.Builder presignerBuilder = S3Presigner.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(credentialsProvider)
                .serviceConfiguration(s3Configuration);
        if (properties.endpoint() != null) {
            clientBuilder.endpointOverride(properties.endpoint());
            presignerBuilder.endpointOverride(properties.endpoint());
        }
        this.s3Client = clientBuilder.build();
        this.s3Presigner = presignerBuilder.build();
    }

    @PostConstruct
    void initializeBucket() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(properties.bucket()).build());
        } catch (NoSuchBucketException exception) {
            if (!properties.createBucket()) {
                throw new StorageException("Storage bucket does not exist.", exception);
            }
            s3Client.createBucket(CreateBucketRequest.builder().bucket(properties.bucket()).build());
        } catch (RuntimeException exception) {
            if (!properties.createBucket()) {
                throw new StorageException("Storage bucket is unavailable.", exception);
            }
            try {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(properties.bucket()).build());
            } catch (RuntimeException createException) {
                throw new StorageException("Storage bucket initialization failed.", createException);
            }
        }
    }

    @Override
    public StoredObject upload(StorageUpload command) {
        try {
            byte[] bytes = command.inputStream().readAllBytes();
            long sizeBytes = bytes.length;
            LOGGER.info(
                    "Uploading object to S3 storageKey={} bucket={} sizeBytes={} pathStyle={} chunkedEncoding=false",
                    command.storageKey(),
                    properties.bucket(),
                    sizeBytes,
                    properties.pathStyle());
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(properties.bucket())
                            .key(command.storageKey())
                            .contentType(command.contentType())
                            .contentLength(sizeBytes)
                            .build(),
                    RequestBody.fromBytes(bytes));
            return new StoredObject(command.storageKey(), command.contentType(), sizeBytes);
        } catch (IOException exception) {
            throw new StorageException("Object upload input could not be read.", exception);
        } catch (RuntimeException exception) {
            throw new StorageException("Object upload failed.", exception);
        }
    }

    @Override
    public StoredObjectDownload download(String storageKey) {
        try {
            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(storageKey)
                    .build());
            return new StoredObjectDownload(
                    response.response().contentType(),
                    response.response().contentLength(),
                    response);
        } catch (RuntimeException exception) {
            throw new StorageException("Object download failed.", exception);
        }
    }

    @Override
    public URI createDownloadUrl(String storageKey, String downloadFileName, Duration ttl) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(storageKey)
                    .build();
            URI url = URI.create(s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                            .signatureDuration(ttl)
                            .getObjectRequest(getObjectRequest)
                            .build())
                    .url()
                    .toString());
            LOGGER.info(
                    "Created S3 presigned download URL storageKey={} bucket={} ttl={} pathStyle={} responseOverrides=false",
                    storageKey,
                    properties.bucket(),
                    ttl,
                    properties.pathStyle());
            return url;
        } catch (RuntimeException exception) {
            throw new StorageException("Download URL creation failed.", exception);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(storageKey)
                    .build());
        } catch (RuntimeException exception) {
            throw new StorageException("Object deletion failed.", exception);
        }
    }

    @Override
    public boolean exists(String storageKey) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(storageKey)
                    .build());
            return true;
        } catch (NoSuchKeyException exception) {
            return false;
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
