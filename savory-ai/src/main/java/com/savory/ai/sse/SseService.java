package com.savory.ai.sse;

/**
 * SSE 推送服务（最小接口，Task 3 补齐 connect/close 与实现）。
 */
public interface SseService {
    void send(String sessionId, Object message);
}
