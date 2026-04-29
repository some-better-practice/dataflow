package com.alan.dataflow.pipeline;

import com.alan.dataflow.domain.JobStatus;
import com.alan.dataflow.domain.ProcessingJob;
import com.alan.dataflow.event.StepCompletedEvent;
import com.alan.dataflow.storage.StorageService;
import com.alan.dataflow.storage.StorageZone;
import com.alan.dataflow.tracking.JobTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FormatConversionService {

    private final StorageService storage;
    private final ApplicationEventPublisher eventPublisher;
    private final JobTrackingService jobTrackingService;

    @Value("${dataflow.simulation.conversion-delay-ms:800}")
    private long delayMs;

    /**
     * 執行在 heavy-pool：呼叫外部編譯執行檔做格式轉換，CPU 密集且效能差。
     * 實作重點：ProcessBuilder 執行外部 binary，timeout 保護，stdout/stderr redirect。
     * 執行前先把來源搬到 NFS WORKING zone（速度最快）。
     */
    @Async("heavyPool")
    public void convert(ProcessingJob job) {
        job.transition(JobStatus.CONVERTING);
        log.info("  ▶ [heavy-{}] CONVERT   {} (external binary, slow)",
                Thread.currentThread().getName(), job.getFileKey().toLogLabel());

        // 先複製到 NFS WORKING zone，讓 binary 走本地 I/O
        String workingRef = storage.put(StorageZone.WORKING, job.getFileKey(),
                job.getOriginalFileName(), 1024 * 1024 * 1024L);
        log.debug("    Staged to NFS working zone: {}", workingRef);

        // 模擬執行外部格式轉換 binary
        log.debug("    Exec: /opt/converter/bin/dat2csv --input {} --output /nfs/working/...",
                workingRef);
        sleep(delayMs * 3); // 轉換最慢，模擬 3x 時間

        String csvName = job.getOriginalFileName().replaceAll("\\.[^.]+$", "") + ".csv";
        String stagingRef = storage.put(StorageZone.STAGING, job.getFileKey(), csvName, 64 * 1024 * 1024L);
        job.setStorageRef(stagingRef);

        job.transition(JobStatus.CONVERTED);
        log.info("  ✓ [heavy-{}] CONVERT   {} → {}", Thread.currentThread().getName(),
                job.getFileKey().toLogLabel(), stagingRef);
        jobTrackingService.recordStep(job, JobStatus.CONVERTED);
        eventPublisher.publishEvent(new StepCompletedEvent(this, job, JobStatus.CONVERTED));
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
