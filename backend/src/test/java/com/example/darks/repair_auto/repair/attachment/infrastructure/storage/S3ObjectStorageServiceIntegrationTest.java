package com.example.darks.repair_auto.repair.attachment.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.springframework.util.unit.DataSize;

@Testcontainers
class S3ObjectStorageServiceIntegrationTest {

    private static final String ACCESS_KEY = "repairauto";
    private static final String SECRET_KEY = "repairauto-secret";

    @Container
    static final GenericContainer<?> MINIO = new GenericContainer<>(
            DockerImageName.parse("minio/minio:RELEASE.2025-04-22T22-12-26Z"))
            .withEnv("MINIO_ROOT_USER", ACCESS_KEY)
            .withEnv("MINIO_ROOT_PASSWORD", SECRET_KEY)
            .withCommand("server", "/data")
            .withExposedPorts(9000);

    @Test
    void givenMinioWhenUploadingPresigningAndDeletingThenS3CompatibleStorageWorks() throws Exception {
        StorageProperties properties = new StorageProperties(
                true,
                URI.create("http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000)),
                "us-east-1",
                "repairauto-private-test",
                ACCESS_KEY,
                SECRET_KEY,
                true,
                true,
                Duration.ofMinutes(10),
                DataSize.ofMegabytes(10),
                30,
                10);
        S3ObjectStorageService storage = new S3ObjectStorageService(properties);
        storage.initializeBucket();

        byte[] content = new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x01};
        String key = "repair-requests/1/completion-photo/test-object";
        StoredObject stored = storage.upload(new StorageUpload(
                key,
                "image/jpeg",
                content.length,
                new ByteArrayInputStream(content)));

        assertThat(stored.storageKey()).isEqualTo(key);
        assertThat(storage.exists(key)).isTrue();

        URI downloadUrl = storage.createDownloadUrl(key, "completion.jpg", Duration.ofMinutes(5));
        HttpURLConnection connection = (HttpURLConnection) downloadUrl.toURL().openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        assertThat(connection.getResponseCode()).isEqualTo(200);
        assertThat(connection.getInputStream().readAllBytes()).isEqualTo(content);

        storage.delete(key);
        assertThat(storage.exists(key)).isFalse();
    }
}
