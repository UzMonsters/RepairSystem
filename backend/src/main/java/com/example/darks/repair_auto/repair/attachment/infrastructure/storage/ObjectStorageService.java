package com.example.darks.repair_auto.repair.attachment.infrastructure.storage;

import java.net.URI;
import java.time.Duration;

public interface ObjectStorageService {

    StoredObject upload(StorageUpload command);

    StoredObjectDownload download(String storageKey);

    URI createDownloadUrl(String storageKey, String downloadFileName, Duration ttl);

    void delete(String storageKey);

    boolean exists(String storageKey);
}
