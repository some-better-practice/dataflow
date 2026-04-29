package com.alan.dataflow.pipeline;

import com.alan.dataflow.domain.JobStatus;
import com.alan.dataflow.domain.ProcessingJob;
import com.alan.dataflow.event.StepCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FtpUploadService {

    private final ApplicationEventPublisher eventPublisher;

    @Value("${dataflow.simulation.upload-delay-ms:200}")
    private long delayMs;

    /**
     * 執行在 light-pool：I/O 密集。
     * 上傳策略：先傳到 .partial 臨時名，完成後 RNFR/RNTO rename，
     * 目的端可用檔名判斷完整性，不會讀到傳到一半的檔案。
     */
    @Async("lightPool")
    public void upload(ProcessingJob job) {
        job.transition(JobStatus.UPLOADING);
        log.info("  ▶ [light-{}] UPLOAD    {} → downstream FTP",
                Thread.currentThread().getName(), job.getFileKey().toLogLabel());

        String outputFile = job.getOriginalFileName()
                .replaceAll("\\.[^.]+$", "") + "_result.csv";

        log.debug("    FTP STOR as {}.partial", outputFile);
        sleep(delayMs * 2);
        log.debug("    FTP RNFR {}.partial → RNTO {} (atomic)", outputFile, outputFile);
        sleep(delayMs);

        job.transition(JobStatus.DONE);
        log.info("  ✓ [light-{}] UPLOAD    {} DONE ✓", Thread.currentThread().getName(),
                job.getFileKey().toLogLabel());
        log.info("  ─────────────────────────────────────────────");
        job.getCheckpoints().forEach(cp -> log.info("    checkpoint: {}", cp));
        log.info("  ─────────────────────────────────────────────");

        eventPublisher.publishEvent(new StepCompletedEvent(this, job, JobStatus.DONE));
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
