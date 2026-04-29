package com.alan.dataflow.event;

import com.alan.dataflow.domain.ProcessingJob;
import org.springframework.context.ApplicationEvent;

/**
 * 模擬 NATS subject: pipeline.{customer}.{product}.{factory}.file.detected
 * FTP Poller 偵測到新檔案時發出，不含實際資料，只有 job 參照（claim-check pattern）
 */
public class FileDetectedEvent extends ApplicationEvent {

    private final ProcessingJob job;

    public FileDetectedEvent(Object source, ProcessingJob job) {
        super(source);
        this.job = job;
    }

    public ProcessingJob getJob() {
        return job;
    }
}
