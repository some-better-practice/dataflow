package com.alan.dataflow.pipeline.parser;

import com.alan.dataflow.constants.ChannelInfo;

/**
 * 策略介面：每個實作宣告自己支援哪個 Channel，並實作解析邏輯。
 *
 * OCP 保證：
 *   - 修改現有 channel 邏輯 → 只改對應的 impl class
 *   - 新增 channel         → 加新 impl class + @Component，不動此介面或 Registry
 */
public interface ContentParser {

    /** 宣告此 Parser 支援的 Channel（用於 Registry 自動路由） */
    ChannelInfo supportedChannel();

    /** 將原始資料解析成正規化的 ParsedContent */
    ParsedContent parse(RawContent raw);
}
