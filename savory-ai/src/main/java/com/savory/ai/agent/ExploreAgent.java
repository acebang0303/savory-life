package com.savory.ai.agent;

import com.savory.ai.config.AgentProperties;
import com.savory.ai.tool.ExploreTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * 探店助手Agent（ReAct模式）
 * 通过多轮推理和工具调用帮助用户规划约会/聚餐/出游路线
 *
 * 工作流程: Thought → Action → Observation → … → Final Answer
 * 设置最大8轮循环和30秒超时
 */
@Component
@Slf4j
public class ExploreAgent {

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private ExploreTools exploreTools;

    @Autowired
    private AgentProperties agentProperties;

    private static final String SYSTEM_PROMPT = """
            你是知味生活的探店助手，帮助用户规划本地的约会、聚餐、出游路线。

            你可以使用以下工具来帮助用户：
            - semanticSearchRestaurant: 根据用户需求语义搜索合适的餐厅
            - getUserPreferenceTags: 获取用户的美食偏好标签
            - getNearbyPOI: 搜索附近的兴趣地点
            - getWeather: 获取天气信息

            回答要求：
            1. 每次规划要考虑餐厅的口味匹配度、环境氛围、人均消费是否在预算内
            2. 给出3个详细的路线方案供用户选择
            3. 每个方案包含：餐厅推荐 + 周边活动/地点 + 建议时间安排
            """;

    /**
     * 执行Agent对话（SSE流式输出）
     *
     * @param message 用户输入的自然语言消息
     * @return 流式回答
     */
    public Flux<String> execute(String message) {
        log.info("探店Agent开始处理: {}", message);

        //1、创建 ChatClient 并注册工具
        MethodToolCallbackProvider provider = MethodToolCallbackProvider.builder()
                .toolObjects(exploreTools)
                .build();

        ChatClient client = ChatClient.builder(chatModel)
                .defaultTools(provider.getToolCallbacks())
                .build();

        //2、启动 ReAct 循环，流式输出
        return client.prompt()
                .system(SYSTEM_PROMPT + convergenceConstraint())
                .user(message)
                .stream()
                .content()
                .timeout(Duration.ofSeconds(agentProperties.getTimeoutSeconds()));
    }

    /**
     * 收敛约束：把 agent.max-iterations 注入 prompt，防止 Agent 反复调用工具陷入死循环
     */
    private String convergenceConstraint() {
        return """


                收敛规则（务必遵守）：
                1. 本轮对话最多调用工具 %d 次，达到上限后必须基于已有结果直接给出完整回答
                2. 同一工具连续调用不要超过 2 次
                3. 只要已获得足够信息（餐厅、天气、地点等），立即停止调用工具并输出最终方案
                """.formatted(agentProperties.getMaxIterations());
    }
}
