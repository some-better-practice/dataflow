package com.alan.dataflow.pipeline;

import com.alan.dataflow.domain.JobStatus;
import com.alan.dataflow.domain.ProcessingJob;
import com.alan.dataflow.event.StepCompletedEvent;
import com.alan.dataflow.storage.StorageService;
import com.alan.dataflow.storage.StorageZone;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompressionService {

    private final StorageService storage;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${dataflow.simulation.compression-delay-ms:150}")
    private long delayMs;

    /**
     * 壓縮 + 備份是獨立步驟（fire-and-forget 備份）。
     * 備份走非同步上傳到 ARCHIVE zone，不卡主流程。
     */
    @Async("lightPool")
    public void compressAndBackup(ProcessingJob job) {
        job.transition(JobStatus.COMPRESSING);
        log.info("  ▶ [light-{}] COMPRESS  {}", Thread.currentThread().getName(),
                job.getFileKey().toLogLabel());

        sleep(delayMs);

        // 套用 naming convention（某些客戶需要 rename）
        String namedFile = applyNamingConvention(job);
        log.debug("    Naming: {} → {}", job.getOriginalFileName(), namedFile);

        // 壓縮寫入 RAW zone（source of truth）
        String compressedRef = storage.put(StorageZone.RAW, job.getFileKey(),
                namedFile + ".gz", 256 * 1024 * 1024L);
        job.setStorageRef(compressedRef);

        // 非同步備份到 ARCHIVE（不等完成，不卡流程）
        asyncBackup(job, compressedRef, namedFile);

        job.transition(JobStatus.COMPRESSED);
        log.info("  ✓ [light-{}] COMPRESS  {} → {}", Thread.currentThread().getName(),
                job.getFileKey().toLogLabel(), compressedRef);
        eventPublisher.publishEvent(new StepCompletedEvent(this, job, JobStatus.COMPRESSED));
    }

    @Async("lightPool")
    public void asyncBackup(ProcessingJob job, String srcRef, String fileName) {
        sleep(delayMs);
        storage.put(StorageZone.ARCHIVE, job.getFileKey(), fileName + ".gz", 256 * 1024 * 1024L);
        log.debug("    [BACKUP] fire-and-forget archive completed for {}", fileName);
    }

    private String applyNamingConvention(ProcessingJob job) {
        com.alan.dataflow.domain.FileKey k = job.getFileKey();
        // 命名規則：{customer}_{factory}_{product}_{dataType}_{originalName}
        return "%s_%s_%s_%s_%s".formatted(
                k.customerId(), k.factoryId(), k.productId(), k.dataType(),
                job.getOriginalFileName());
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
