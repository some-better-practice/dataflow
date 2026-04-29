package com.alan.dataflow.pipeline.parser;

import com.alan.dataflow.constants.ChannelInfo;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Spring 自動收集所有 ContentParser @Component，建立 channel → parser 的路由表。
 *
 * 呼叫方只需 registry.get(ChannelInfo.XXX).parse(raw)，
 * 完全不知道背後是哪個 parser class。
 */
@Slf4j
@Service
public class ContentParserRegistry {

    private final Map<ChannelInfo, ContentParser> registry;

    public ContentParserRegistry(List<ContentParser> parsers) {
        registry = parsers.stream()
                .collect(Collectors.toMap(
                        ContentParser::supportedChannel,
                        Function.identity(),
                        (a, b) -> { throw new IllegalStateException(
                            "Duplicate parser for channel: " + a.supportedChannel()); }
                ));
    }

    @PostConstruct
    void logRegistered() {
        log.info("[ContentParserRegistry] {} parsers registered:", registry.size());
        registry.forEach((channel, parser) ->
            log.info("  {} → {}",
                channel.name(), parser.getClass().getSimpleName()));
    }

    public ContentParser get(ChannelInfo channel) {
        ContentParser parser = registry.get(channel);
        if (parser == null) {
            throw new IllegalStateException(
                "No ContentParser registered for channel: " + channel);
        }
        return parser;
    }

    public boolean supports(ChannelInfo channel) {
        return registry.containsKey(channel);
    }
}
