package com.alan.dataflow.event;

import org.springframework.context.ApplicationEvent;

/**
 * 某 job 的相依條件已滿足，可以從 WAITING_DEPENDENCY 繼續
 */
public class DependencyReadyEvent extends ApplicationEvent {

    private final String unblockedJobId;

    public DependencyReadyEvent(Object source, String unblockedJobId) {
        super(source);
        this.unblockedJobId = unblockedJobId;
    }

    public String getUnblockedJobId() { return unblockedJobId; }
}
