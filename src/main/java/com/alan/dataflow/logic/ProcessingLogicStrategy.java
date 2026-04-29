package com.alan.dataflow.logic;

import com.alan.dataflow.domain.ProcessingJob;
import com.alan.dataflow.pipeline.parser.ParsedContent;

/**
 * 策略介面：各廠區、各資料類型的處理邏輯各自實作
 */
public interface ProcessingLogicStrategy {

    /** 回傳此策略能處理的 routing key pattern，支援 * 萬用字元 */
    String routingPattern();

    /** 執行實際資料處理，吃 ParsedContent（已正規化的資料列），回傳輸出檔路徑 */
    String process(ProcessingJob job, ParsedContent parsed);

    default String name() {
        return getClass().getSimpleName();
    }
}
