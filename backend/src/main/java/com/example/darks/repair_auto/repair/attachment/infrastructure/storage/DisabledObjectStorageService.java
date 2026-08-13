package com.example.darks.repair_auto.repair.attachment.infrastructure.storage;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "app.storage", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DisabledObjectStorageService implements ObjectStorageService {

    @Override
    public StoredObject upload(StorageUpload command) {
        throw new StorageException("Object storage is disabled.");
    }

    @Override
    public StoredObjectDownload download(String storageKey) {
        throw new StorageException("Object storage is disabled.");
    }

    @Override
    public URI createDownloadUrl(String storageKey, String downloadFileName, Duration ttl) {
        throw new StorageException("Object storage is disabled.");
    }

    @Override
    public void delete(String storageKey) {
    }

    @Override
    public boolean exists(String storageKey) {
        return false;
    }
}
