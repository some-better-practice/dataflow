package com.alan.dataflow.domain;

import com.alan.dataflow.constants.ChannelInfo;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Data
@Builder
public class ProcessingJob {

    private final String jobId;
    private final FileKey fileKey;
    private final ChannelInfo channelInfo;
    private final String originalFileName;

    /** claim-check pattern: 訊息只存路徑，實際資料在物件儲存 */
    private volatile String storageRef;

    private final AtomicReference<JobStatus> status =
            new AtomicReference<>(JobStatus.PENDING);

    private final List<String> checkpoints = new ArrayList<>();
    private volatile Instant lastCheckpointAt;

    private final Instant createdAt = Instant.now();

    public void transition(JobStatus next) {
        JobStatus prev = status.getAndSet(next);
        String msg = "[%s] %s → %s".formatted(jobId, prev, next);
        checkpoints.add(msg);
        lastCheckpointAt = Instant.now();
    }

    public JobStatus getStatus() {
        return status.get();
    }
}
