package com.alan.dataflow.simulation;

import com.alan.dataflow.constants.ChannelInfo;
import com.alan.dataflow.dependency.DependencyRule;
import com.alan.dataflow.dependency.DependencyTracker;
import com.alan.dataflow.domain.FileKey;
import com.alan.dataflow.domain.ProcessingJob;
import com.alan.dataflow.event.FileDetectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 模擬場景：
 *
 * Job-A  ALAN / N5  / FAB12 / WAT  / INLINE  → 嚴格閾值邏輯
 *         ↑ 相依 Job-B 完成後才能進 CONVERT
 *
 * Job-B  ALAN / N5  / FAB12 / SPC  / FINAL   → 滾動平均邏輯（先跑完）
 *
 * Job-C  ALAN / N3  / FAB14 / WAT  / INLINE  → 寬鬆閾值 + 正規化邏輯（獨立）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SimulationRunner implements CommandLineRunner {

    private final ApplicationEventPublisher eventPublisher;
    private final DependencyTracker dependencyTracker;

    @Override
    public void run(String... args) throws Exception {
        printBanner();

        // ── Job-B: SPC data（先發，Job-A 相依它）
        ProcessingJob jobB = buildJob("job-B",
                new FileKey("ALAN", "N5", "FAB12", "SPC", "FINAL"),
                ChannelInfo.CHANNEL_B,
                "SPC_N5_FAB12_20240101.dat");

        // ── Job-A: WAT data，相依 Job-B
        ProcessingJob jobA = buildJob("job-A",
                new FileKey("ALAN", "N5", "FAB12", "WAT", "INLINE"),
                ChannelInfo.CHANNEL_A,
                "WAT_N5_FAB12_20240101.dat");

        // ── Job-C: N3/FAB14 WAT，完全獨立
        ProcessingJob jobC = buildJob("job-C",
                new FileKey("ALAN", "N3", "FAB14", "WAT", "INLINE"),
                ChannelInfo.CHANNEL_C,
                "WAT_N3_FAB14_20240101.dat");

        // 註冊相依規則：job-A 必須等 job-B 完成
        dependencyTracker.register(new DependencyRule(
                jobA.getJobId(), jobA.getFileKey(),
                jobB.getJobId(),
                "WAT INLINE requires SPC FINAL to be processed first (FAB12 rule)"));

        log.info("");
        log.info("════════════════════════════════════════════════════");
        log.info("  Firing 3 jobs concurrently...");
        log.info("  job-A (WAT/FAB12) waits for job-B (SPC/FAB12)");
        log.info("  job-C (WAT/FAB14) runs independently");
        log.info("════════════════════════════════════════════════════");
        log.info("");

        // 三個 job 同時觸發
        eventPublisher.publishEvent(new FileDetectedEvent(this, jobA));
        eventPublisher.publishEvent(new FileDetectedEvent(this, jobB));
        eventPublisher.publishEvent(new FileDetectedEvent(this, jobC));

        // 等待所有 job 完成（實際系統不需要這個，這裡只是讓 demo 不要提早退出）
        TimeUnit.SECONDS.sleep(30);

        log.info("");
        log.info("════════════════════════════════════════════════════");
        log.info("  Simulation complete.");
        log.info("════════════════════════════════════════════════════");
    }

    private ProcessingJob buildJob(String id, FileKey key, ChannelInfo channelInfo, String fileName) {
        return ProcessingJob.builder()
                .jobId(id + "-" + UUID.randomUUID().toString().substring(0, 6))
                .fileKey(key)
                .channelInfo(channelInfo)
                .originalFileName(fileName)
                .build();
    }

    private void printBanner() {
        log.info("");
        log.info("╔══════════════════════════════════════════════════════╗");
        log.info("║      Alan Data Flow Pipeline Simulation              ║");
        log.info("║                                                      ║");
        log.info("║  FTP Download → Compress/Backup → Format Convert    ║");
        log.info("║  → CSV Process (logic routing) → FTP Upload          ║");
        log.info("║                                                      ║");
        log.info("║  Worker pools:  light(8)  heavy(3)  aggr(2)         ║");
        log.info("║  Storage:       MinIO (raw/staging/processed)        ║");
        log.info("║                 NFS   (working, fast I/O)            ║");
        log.info("╚══════════════════════════════════════════════════════╝");
        log.info("");
    }
}
