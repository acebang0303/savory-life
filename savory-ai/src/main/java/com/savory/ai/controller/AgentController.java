package com.savory.ai.controller;

import com.savory.ai.agent.AuditAgent;
import com.savory.ai.agent.ExploreAgent;
import com.savory.ai.agent.JChatMind;
import com.savory.ai.agent.MerchantAgent;
import com.savory.ai.dto.AgentEvent;
import com.savory.ai.sse.SseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * AI Agent 对话接口（SSE 推送式）。
 * 建立连接后异步执行 Agent Loop，运行时内部通过 SseService 主动推送。
 */
@RestController
@RequestMapping("/ai")
@Slf4j
public class AgentController {

    private static final Executor EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final ExploreAgent exploreAgent;
    private final MerchantAgent merchantAgent;
    private final AuditAgent auditAgent;
    private final SseService sseService;

    public AgentController(ExploreAgent exploreAgent,
                           MerchantAgent merchantAgent,
                           AuditAgent auditAgent,
                           SseService sseService) {
        this.exploreAgent = exploreAgent;
        this.merchantAgent = merchantAgent;
        this.auditAgent = auditAgent;
        this.sseService = sseService;
    }

    @GetMapping(value = "/agent/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter exploreChat(@RequestParam String message,
                                  @RequestParam(defaultValue = "deepseek") String model) {
        String sessionId = UUID.randomUUID().toString();
        SseEmitter emitter = sseService.connect(sessionId);
        EXECUTOR.execute(() -> {
            try {
                exploreAgent.execute(model, sessionId, message).run();
            } catch (Exception e) {
                log.error("探店助手执行失败", e);
                sseService.send(sessionId, new AgentEvent("error", e.getMessage()));
            } finally {
                sseService.close(sessionId);
            }
        });
        return emitter;
    }

    @GetMapping(value = "/merchant/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter merchantChat(@RequestParam String question,
                                   @RequestParam Long empId,
                                   @RequestParam(defaultValue = "deepseek") String model) {
        String sessionId = UUID.randomUUID().toString();
        SseEmitter emitter = sseService.connect(sessionId);
        EXECUTOR.execute(() -> {
            try {
                JChatMind runtime = merchantAgent.execute(model, sessionId, question, empId);
                if (runtime == null) {
                    sseService.send(sessionId, new AgentEvent("message",
                            "未找到对应的商户信息，请联系管理员确认账号绑定。"));
                } else {
                    runtime.run();
                }
            } catch (Exception e) {
                log.error("商家助手执行失败", e);
                sseService.send(sessionId, new AgentEvent("error", e.getMessage()));
            } finally {
                sseService.close(sessionId);
            }
        });
        return emitter;
    }

    @PostMapping("/audit/content")
    public Object contentAudit(@RequestParam String content,
                               @RequestParam(defaultValue = "note") String contentType) {
        log.info("AI内容审核: type={}", contentType);
        return auditAgent.audit(content, contentType);
    }
}
