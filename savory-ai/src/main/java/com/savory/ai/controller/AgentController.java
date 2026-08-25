package com.savory.ai.controller;

import com.savory.ai.agent.AuditAgent;
import com.savory.ai.agent.ExploreAgent;
import com.savory.ai.agent.MerchantAgent;
import com.savory.ai.dto.AgentEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * AI Agent 对话接口（SSE流式输出）
 * Event 类型: thinking → message → done
 */
@RestController
@RequestMapping("/ai")
@Slf4j
public class AgentController {

    @Autowired
    private ExploreAgent exploreAgent;

    @Autowired
    private MerchantAgent merchantAgent;

    @Autowired
    private AuditAgent auditAgent;

    @Autowired
    private MeterRegistry meterRegistry;

    /**
     * 探店助手Agent对话（SSE流式输出）
     */
    @GetMapping(value = "/agent/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AgentEvent>> exploreChat(@RequestParam String message) {
        log.info("探店助手SSE对话: {}", message);
        Timer.Sample sample = Timer.start(meterRegistry);
        return toSse(exploreAgent.execute(message))
                .doFinally(sig -> sample.stop(meterRegistry.timer("savory.ai.agent.explore")));
    }

    /**
     * 商家经营助手对话（SSE流式输出）
     */
    @GetMapping(value = "/merchant/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AgentEvent>> merchantChat(@RequestParam String question,
                                                          @RequestParam Long empId) {
        log.info("商家经营助手对话: empId={}, question={}", empId, question);
        Timer.Sample sample = Timer.start(meterRegistry);
        return toSse(merchantAgent.execute(question, empId))
                .doFinally(sig -> sample.stop(meterRegistry.timer("savory.ai.agent.merchant")));
    }

    /**
     * AI内容审核（内部调用接口）
     */
    @PostMapping("/audit/content")
    public Object contentAudit(@RequestParam String content,
                               @RequestParam(defaultValue = "note") String contentType) {
        log.info("AI内容审核: type={}", contentType);
        return auditAgent.audit(content, contentType);
    }

    /**
     * 将裸 token 流组装为结构化 SSE 事件（thinking → message → done）
     * message 按句/段合并，避免逐字输出
     */
    private Flux<ServerSentEvent<AgentEvent>> toSse(Flux<String> content) {
        Flux<AgentEvent> messageEvents = content
                .filter(c -> c != null && !c.isEmpty())
                .bufferUntil(this::isSentenceEnd)
                .map(chunks -> new AgentEvent("message", String.join("", chunks)))
                .filter(e -> !e.content().isEmpty());

        return Flux.concat(
                Mono.just(event(new AgentEvent("thinking", "正在思考..."))),
                messageEvents.map(e -> event(e)),
                Mono.just(event(new AgentEvent("done", "")))
        );
    }

    private boolean isSentenceEnd(String chunk) {
        return chunk.endsWith("。") || chunk.endsWith("！")
                || chunk.endsWith("？") || chunk.endsWith("\n");
    }

    private ServerSentEvent<AgentEvent> event(AgentEvent e) {
        return ServerSentEvent.builder(e).event(e.type()).build();
    }
}
