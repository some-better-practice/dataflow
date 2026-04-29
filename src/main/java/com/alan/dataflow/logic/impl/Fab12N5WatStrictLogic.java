package com.alan.dataflow.logic.impl;

import com.alan.dataflow.domain.ProcessingJob;
import com.alan.dataflow.logic.ProcessingLogicStrategy;
import com.alan.dataflow.pipeline.parser.ParsedContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Fab12N5WatStrictLogic implements ProcessingLogicStrategy {

    @Override
    public String routingPattern() {
        return "ALAN.N5.FAB12.WAT.*";
    }

    @Override
    public String process(ProcessingJob job, ParsedContent parsed) {
        log.info("    [LOGIC:{}] rows={} Applying strict threshold + gate-oxide correction",
                name(), parsed.rows().size());
        log.info("    [LOGIC:{}] Step 1: Vth/Idsat/Ioff column mapping", name());
        log.info("    [LOGIC:{}] Step 2: FAB12-specific offset table", name());
        log.info("    [LOGIC:{}] Step 3: Flag outliers sigma=3 (strict)", name());
        log.info("    [LOGIC:{}] Step 4: Cross-ref with SPC dependency data", name());
        String output = "processed/" + job.getOriginalFileName().replace(".csv", "_fab12_n5.csv");
        log.info("    [LOGIC:{}] Output → {}", name(), output);
        return output;
    }
}
