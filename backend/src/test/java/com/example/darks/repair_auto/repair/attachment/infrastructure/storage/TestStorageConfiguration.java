package com.example.darks.repair_auto.repair.attachment.infrastructure.storage;

import com.example.darks.repair_auto.shared.error.ResourceNotFoundException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestStorageConfiguration {

    @Bean
    @Primary
    public ObjectStorageService objectStorageService() {
        return new FakeObjectStorageService();
    }

    public static class FakeObjectStorageService implements ObjectStorageService {
        private final Map<String, StoredData> objects = new ConcurrentHashMap<>();

        public record StoredData(String contentType, byte[] bytes) {}

        @Override
        public StoredObject upload(StorageUpload command) {
            try {
                byte[] bytes = command.inputStream().readAllBytes();
                objects.put(command.storageKey(), new StoredData(command.contentType(), bytes));
                return new StoredObject(command.storageKey(), command.contentType(), command.sizeBytes());
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
        }

        @Override
        public StoredObjectDownload download(String storageKey) {
            StoredData data = objects.get(storageKey);
            if (data == null) {
                throw new ResourceNotFoundException("Storage object not found.");
            }
            return new StoredObjectDownload(data.contentType(), data.bytes().length, new ByteArrayInputStream(data.bytes()));
        }

        @Override
        public URI createDownloadUrl(String storageKey, String downloadFileName, Duration ttl) {
            return URI.create("https://storage.test/" + downloadFileName);
        }

        @Override
        public void delete(String storageKey) {
            objects.remove(storageKey);
        }

        @Override
        public boolean exists(String storageKey) {
            return objects.containsKey(storageKey);
        }
    }
}