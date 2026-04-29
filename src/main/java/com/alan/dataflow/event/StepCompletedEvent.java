package com.alan.dataflow.event;

import com.alan.dataflow.domain.JobStatus;
import com.alan.dataflow.domain.ProcessingJob;
import org.springframework.context.ApplicationEvent;

/**
 * 模擬 NATS subject: pipeline.step.completed
 * 每個步驟完成後發出，Orchestrator 監聽後決定下一步
 */
public class StepCompletedEvent extends ApplicationEvent {

    private final ProcessingJob job;
    private final JobStatus completedStep;

    public StepCompletedEvent(Object source, ProcessingJob job, JobStatus completedStep) {
        super(source);
        this.job = job;
        this.completedStep = completedStep;
    }

    public ProcessingJob getJob() { return job; }
    public JobStatus getCompletedStep() { return completedStep; }
}
