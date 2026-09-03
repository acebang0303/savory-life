package com.savory.ai.sse;

import com.savory.ai.dto.AgentEvent;
import org.springframework.stereotype.Component;

/**
 * 流式事件通道：包装现有 SseService 推送到前端。
 */
@Component
public class SseEventSink implements AgentEventSink {

    private final SseService sseService;

    public SseEventSink(SseService sseService) {
        this.sseService = sseService;
    }

    @Override
    public void send(String sessionId, AgentEvent event) {
        sseService.send(sessionId, event);
    }
}
