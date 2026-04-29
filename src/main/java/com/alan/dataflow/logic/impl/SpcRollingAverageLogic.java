package com.alan.dataflow.logic.impl;

import com.alan.dataflow.domain.ProcessingJob;
import com.alan.dataflow.logic.ProcessingLogicStrategy;
import com.alan.dataflow.pipeline.parser.ParsedContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SpcRollingAverageLogic implements ProcessingLogicStrategy {

    @Override
    public String routingPattern() {
        return "ALAN.*.*.SPC.*";
    }

    @Override
    public String process(ProcessingJob job, ParsedContent parsed) {
        String subType = job.getFileKey().subType();
        log.info("    [LOGIC:{}] rows={} SPC subType={} rolling-average control chart",
                name(), parsed.rows().size(), subType);
        log.info("    [LOGIC:{}] Step 1: Parse SPC measurement columns", name());
        log.info("    [LOGIC:{}] Step 2: Compute UCL/LCL (±3σ rolling window=25)", name());
        if ("FINAL".equals(subType)) {
            log.info("    [LOGIC:{}] Step 3: Final yield correlation analysis", name());
        } else {
            log.info("    [LOGIC:{}] Step 3: Inline defect density trend", name());
        }
        String output = "processed/" + job.getOriginalFileName().replace(".csv", "_spc.csv");
        log.info("    [LOGIC:{}] Output → {}", name(), output);
        return output;
    }
}
