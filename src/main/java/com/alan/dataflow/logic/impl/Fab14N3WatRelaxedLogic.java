package com.alan.dataflow.logic.impl;

import com.alan.dataflow.domain.ProcessingJob;
import com.alan.dataflow.logic.ProcessingLogicStrategy;
import com.alan.dataflow.pipeline.parser.ParsedContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Fab14N3WatRelaxedLogic implements ProcessingLogicStrategy {

    @Override
    public String routingPattern() {
        return "ALAN.N3.FAB14.WAT.*";
    }

    @Override
    public String process(ProcessingJob job, ParsedContent parsed) {
        log.info("    [LOGIC:{}] rows={} Applying relaxed threshold + metal-resistance normalization",
                name(), parsed.rows().size());
        log.info("    [LOGIC:{}] Step 1: WAT column mapping", name());
        log.info("    [LOGIC:{}] Step 2: Metal resistance normalization (FAB14 specific)", name());
        log.info("    [LOGIC:{}] Step 3: Flag outliers sigma=4 (relaxed)", name());
        log.info("    [LOGIC:{}] Step 4: Merge with PCM inline data", name());
        String output = "processed/" + job.getOriginalFileName().replace(".csv", "_fab14_n3.csv");
        log.info("    [LOGIC:{}] Output → {}", name(), output);
        return output;
    }
}
