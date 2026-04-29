package com.alan.dataflow.storage;

import com.alan.dataflow.domain.FileKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模擬 MinIO (物件儲存) + NFS (工作區) 混用
 * 實際實作: MinIO SDK for RAW/STAGING/PROCESSED/ARCHIVE，NFS FileChannel for WORKING
 */
@Slf4j
@Service
public class StorageService {

    // 模擬物件儲存 bucket: zone -> objectKey -> content-size
    private final Map<String, Map<String, Long>> store = new ConcurrentHashMap<>();

    public String put(StorageZone zone, FileKey key, String fileName, long sizeBytes) {
        String objectKey = buildObjectKey(key, fileName);
        store.computeIfAbsent(zone.name(), z -> new ConcurrentHashMap<>())
             .put(objectKey, sizeBytes);

        String backend = (zone == StorageZone.WORKING) ? "NFS" : "MinIO";
        log.debug("  [{}] PUT {}/{} ({} bytes)", backend, zone, objectKey, sizeBytes);
        return zone.name() + "/" + objectKey;
    }

    public boolean exists(StorageZone zone, FileKey key, String fileName) {
        String objectKey = buildObjectKey(key, fileName);
        return store.getOrDefault(zone.name(), Map.of()).containsKey(objectKey);
    }

    public void move(String srcRef, StorageZone destZone, FileKey key, String destFileName) {
        String destKey = buildObjectKey(key, destFileName);
        store.computeIfAbsent(destZone.name(), z -> new ConcurrentHashMap<>())
             .put(destKey, 0L);
        log.debug("  [MinIO] MOVE {} → {}/{}", srcRef, destZone, destKey);
    }

    private String buildObjectKey(FileKey key, String fileName) {
        // path: customer/product/factory/dataType/filename
        return "%s/%s/%s/%s/%s".formatted(
                key.customerId(), key.productId(),
                key.factoryId(), key.dataType(), fileName);
    }
}
