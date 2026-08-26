package com.savory.ai.agent;

import com.savory.ai.dto.AgentEvent;
import com.savory.ai.sse.SseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 手写 Agent Loop 运行时（移植自 JChatMind）。
 *
 * 设计要点：
 * 1. 状态机 IDLE→THINKING/EXECUTING→FINISHED/ERROR，MAX_STEPS=20 硬上限防死循环
 * 2. 关闭 Spring AI 内部工具自动执行（internalToolExecutionEnabled=false），
 *    由 ToolCallingManager 手动执行，便于在循环中观察与控制每一步
 * 3. 对话记忆用 ChatMemory（内存窗口），不做 DB 持久化（savory-ai 由上层 ConversationService 负责）
 */
@Slf4j
public class JChatMind {

    private static final Integer MAX_STEPS = 20;
    private static final Integer DEFAULT_MAX_MESSAGES = 20;

    private final String systemPrompt;
    private final ChatClient chatClient;
    private final List<ToolCallback> availableTools;
    private final SseService sseService;
    private final String chatSessionId;

    private AgentState agentState;
    private final ToolCallingManager toolCallingManager;
    private final ChatMemory chatMemory;
    private final ChatOptions chatOptions;
    private ChatResponse lastChatResponse;

    public JChatMind(ChatClient chatClient,
                     String systemPrompt,
                     List<ToolCallback> availableTools,
                     SseService sseService,
                     String chatSessionId) {
        this(chatClient, systemPrompt, availableTools, sseService, chatSessionId, List.of());
    }

    public JChatMind(ChatClient chatClient,
                     String systemPrompt,
                     List<ToolCallback> availableTools,
                     SseService sseService,
                     String chatSessionId,
                     List<Message> memory) {
        this.systemPrompt = systemPrompt;
        this.chatClient = chatClient;
        this.availableTools = availableTools;
        this.sseService = sseService;
        this.chatSessionId = chatSessionId;

        this.agentState = AgentState.IDLE;

        this.chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(DEFAULT_MAX_MESSAGES)
                .build();
        this.chatMemory.add(chatSessionId, memory);

        if (StringUtils.hasLength(systemPrompt)) {
            this.chatMemory.add(chatSessionId, new SystemMessage(systemPrompt));
        }

        // 关闭 Spring AI 内部工具自动执行，改由 ToolCallingManager 手动执行
        this.chatOptions = DefaultToolCallingChatOptions.builder()
                .internalToolExecutionEnabled(false)
                .build();

        this.toolCallingManager = ToolCallingManager.builder().build();
    }

    /**
     * 决策阶段：构建决策 prompt，调用模型，返回是否需要调用工具。
     */
    private boolean think() {
        this.agentState = AgentState.THINKING;

        String thinkPrompt = """
                现在你是一个智能的「决策模块」
                请根据当前对话上下文，决定下一步的动作：是调用工具获取信息，还是直接给出最终回答。
                """;

        Prompt prompt = Prompt.builder()
                .chatOptions(this.chatOptions)
                .messages(this.chatMemory.get(this.chatSessionId))
                .build();

        this.lastChatResponse = this.chatClient
                .prompt(prompt)
                .system(thinkPrompt)
                .toolCallbacks(this.availableTools.toArray(new ToolCallback[0]))
                .call()
                .chatClientResponse()
                .chatResponse();

        AssistantMessage output = this.lastChatResponse.getResult().getOutput();
        List<AssistantMessage.ToolCall> toolCalls = output.getToolCalls();

        if (StringUtils.hasLength(output.getText())) {
            sseService.send(chatSessionId, new AgentEvent("message", output.getText()));
        }

        logToolCalls(toolCalls);
        return !toolCalls.isEmpty();
    }

    /**
     * 执行阶段：手动执行工具调用，更新对话记忆，检查 terminate 工具。
     */
    private void execute() {
        this.agentState = AgentState.EXECUTING;

        Prompt prompt = Prompt.builder()
                .messages(this.chatMemory.get(this.chatSessionId))
                .chatOptions(this.chatOptions)
                .build();

        ToolExecutionResult toolExecutionResult =
                toolCallingManager.executeToolCalls(prompt, this.lastChatResponse);

        this.chatMemory.clear(this.chatSessionId);
        this.chatMemory.add(this.chatSessionId, toolExecutionResult.conversationHistory());

        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) toolExecutionResult
                .conversationHistory()
                .get(toolExecutionResult.conversationHistory().size() - 1);

        String collect = toolResponseMessage.getResponses()
                .stream()
                .map(resp -> "工具 " + resp.name() + " 的返回结果为：" + resp.responseData())
                .collect(Collectors.joining("\n"));

        log.info("工具调用结果：{}", collect);
        sseService.send(chatSessionId, new AgentEvent("action", collect));

        if (toolResponseMessage.getResponses()
                .stream()
                .anyMatch(resp -> resp.name().equals("terminate"))) {
            this.agentState = AgentState.FINISHED;
            log.info("任务结束");
        }
    }

    private void step() {
        if (think()) {
            execute();
        } else {
            agentState = AgentState.FINISHED;
        }
    }

    /**
     * 运行 Agent Loop：最多 MAX_STEPS 轮，直到 FINISHED 或异常。
     */
    public void run() {
        if (agentState != AgentState.IDLE) {
            throw new IllegalStateException("Agent is not idle");
        }

        try {
            for (int i = 0; i < MAX_STEPS && agentState != AgentState.FINISHED; i++) {
                int currentStep = i + 1;
                step();
                if (currentStep >= MAX_STEPS) {
                    agentState = AgentState.FINISHED;
                    log.warn("Max steps reached, stopping agent");
                }
            }
            agentState = AgentState.FINISHED;
        } catch (Exception e) {
            agentState = AgentState.ERROR;
            log.error("Error running agent", e);
            throw new RuntimeException("Error running agent", e);
        }
    }

    public AgentState getAgentState() {
        return agentState;
    }

    private void logToolCalls(List<AssistantMessage.ToolCall> toolCalls) {
        if (toolCalls == null || toolCalls.isEmpty()) {
            log.info("[ToolCalling] 无工具调用");
            return;
        }
        String logMessage = IntStream.range(0, toolCalls.size())
                .mapToObj(i -> {
                    AssistantMessage.ToolCall call = toolCalls.get(i);
                    return String.format("[ToolCalling #%d] name=%s, arguments=%s",
                            i + 1, call.name(), call.arguments());
                })
                .collect(Collectors.joining("\n"));
        log.info("========== Tool Calling ==========\n{}\n=============================", logMessage);
    }
}
