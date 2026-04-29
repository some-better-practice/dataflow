package com.alan.dataflow.pipeline.parser.impl;

import com.alan.dataflow.constants.ChannelInfo;
import com.alan.dataflow.pipeline.parser.ContentParser;
import com.alan.dataflow.pipeline.parser.ParsedContent;
import com.alan.dataflow.pipeline.parser.RawContent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChannelEParser implements ContentParser {

    @Override
    public ChannelInfo supportedChannel() { return ChannelInfo.CHANNEL_E; }

    @Override
    public ParsedContent parse(RawContent raw) {
        return new ParsedContent(supportedChannel(), List.of());
    }
}
