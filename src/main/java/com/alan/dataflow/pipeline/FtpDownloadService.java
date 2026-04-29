package com.alan.dataflow.pipeline;

import com.alan.dataflow.domain.JobStatus;
import com.alan.dataflow.domain.ProcessingJob;
import com.alan.dataflow.event.StepCompletedEvent;
import com.alan.dataflow.storage.StorageService;
import com.alan.dataflow.storage.StorageZone;
import com.alan.dataflow.service.JobTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FtpDownloadService {

    private final StorageService storage;
    private final ApplicationEventPublisher eventPublisher;
    private final JobTrackingService jobTrackingService;

    @Value("${dataflow.simulation.ftp-download-delay-ms:300}")
    private long delayMs;

    /**
     * 執行在 light-pool：I/O 密集，不佔 CPU。
     * 實作重點：TYPE I (binary mode) + REST offset 斷點續傳，下載到 .partial 完成後 rename。
     */
    @Async("lightPool")
    public void download(ProcessingJob job) {
        job.transition(JobStatus.DOWNLOADING);
        log.info("  ▶ [light-{}] DOWNLOAD  {} file={}",
                Thread.currentThread().getName(), job.getFileKey().toLogLabel(), job.getOriginalFileName());

        simulate(delayMs, "  FTP REST resume check... remote_size=1,024MB local_partial=0MB → full download");
        simulate(delayMs, "  FTP TYPE I (binary) RETR → writing to .partial");
        simulate(delayMs, "  atomic rename: .partial → complete");

        // claim-check: 存路徑到物件儲存，job 只持有 ref
        String ref = storage.put(StorageZone.RAW, job.getFileKey(),
                job.getOriginalFileName(), 1024 * 1024 * 1024L);
        job.setStorageRef(ref);
        job.transition(JobStatus.DOWNLOADED);

        log.info("  ✓ [light-{}] DOWNLOAD  {} → ref={}", Thread.currentThread().getName(),
                job.getFileKey().toLogLabel(), ref);
        jobTrackingService.recordStep(job, JobStatus.DOWNLOADED);
        eventPublisher.publishEvent(new StepCompletedEvent(this, job, JobStatus.DOWNLOADED));
    }

    private void simulate(long ms, String msg) {
        log.debug("    {}", msg);
        sleep(ms);
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
