package com.savory.ai.agent;

import org.springframework.stereotype.Component;

/**
 * 探店助手：委托 AgentRuntimeFactory 组装 JChatMind 运行时实例，
 * 由 AgentController 异步执行 run()，SSE 推送式输出。
 */
@Component
public class ExploreAgent {

    private final AgentRuntimeFactory factory;

    public ExploreAgent(AgentRuntimeFactory factory) {
        this.factory = factory;
    }

    public JChatMind execute(String model, String sessionId, String message) {
        return factory.createExplore(model, sessionId, message);
    }
}
