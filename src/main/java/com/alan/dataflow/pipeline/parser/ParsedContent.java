package com.alan.dataflow.pipeline.parser;

import com.alan.dataflow.constants.ChannelInfo;

import java.util.List;
import java.util.Map;

/** Parser 的輸出：正規化後的資料列，供後續處理邏輯使用 */
public record ParsedContent(
        ChannelInfo channel,
        List<Map<String, Object>> rows
) {}
