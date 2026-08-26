package com.savory.ai.agent;

import com.savory.ai.config.AgentProperties;
import com.savory.ai.config.ChatClientRegistry;
import com.savory.ai.nlsql.SqlValidator;
import com.savory.ai.sse.SseService;
import com.savory.ai.tool.ExploreTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 业务 Agent 运行时工厂：把业务 Agent 定义为「角色(systemPrompt) + 工具集」，
 * 按模型 key 从 ChatClientRegistry 取 ChatClient，组装成 JChatMind 运行时实例。
 */
@Component
public class AgentRuntimeFactory {

    private static final String EXPLORE_SYSTEM_PROMPT = """
            你是知味生活的探店助手，帮助用户规划本地的约会、聚餐、出游路线。

            可用工具：
            - semanticSearchRestaurant: 根据用户需求语义搜索合适的餐厅
            - getUserPreferenceTags: 获取用户的美食偏好标签
            - getNearbyPOI: 搜索附近的兴趣地点
            - getWeather: 获取天气信息

            回答要求：
            1. 每次规划要考虑餐厅的口味匹配度、环境氛围、人均消费是否在预算内
            2. 给出3个详细的路线方案供用户选择
            3. 每个方案包含：餐厅推荐 + 周边活动/地点 + 建议时间安排

            收敛规则：
            1. 本轮最多调用工具 8 次，达到上限后必须基于已有结果直接回答
            2. 同一工具连续调用不要超过 2 次
            3. 已获得足够信息时立即停止调用工具并输出最终方案
            """;

    private static final String MERCHANT_SYSTEM_PROMPT = """
            你是餐饮商家的经营数据分析助手。
            可用工具：queryBusinessData（查询销量/营收/评价等经营数据）。
            工作方式：先调用 queryBusinessData 获取数据，再基于数据用自然、专业、简洁的中文回答商家的问题。
            要求：直接给出结论和数据，不提及SQL/表名/字段名；不编造查询结果里没有的数据；适当给出经营建议。
            """;

    private final ChatClientRegistry registry;
    private final SseService sseService;
    private final ExploreTools exploreTools;
    private final SqlValidator sqlValidator;
    private final JdbcTemplate bizJdbcTemplate;
    private final AgentProperties agentProperties;

    public AgentRuntimeFactory(ChatClientRegistry registry,
                               SseService sseService,
                               ExploreTools exploreTools,
                               SqlValidator sqlValidator,
                               @Qualifier("bizJdbcTemplate") JdbcTemplate bizJdbcTemplate,
                               AgentProperties agentProperties) {
        this.registry = registry;
        this.sseService = sseService;
        this.exploreTools = exploreTools;
        this.sqlValidator = sqlValidator;
        this.bizJdbcTemplate = bizJdbcTemplate;
        this.agentProperties = agentProperties;
    }

    public JChatMind createExplore(String model, String sessionId, String message) {
        ChatClient client = registry.get(model);
        List<ToolCallback> tools = Arrays.asList(MethodToolCallbackProvider.builder()
                .toolObjects(exploreTools).build().getToolCallbacks());
        List<Message> memory = List.of(new UserMessage(message));
        return new JChatMind(client, EXPLORE_SYSTEM_PROMPT, tools, sseService, sessionId, memory,
                agentProperties.getMaxIterations(), agentProperties.getTimeoutSeconds());
    }

    public JChatMind createMerchant(String model, String sessionId, Long merchantId, String message) {
        ChatClient client = registry.get(model);
        MerchantQueryTools queryTools = new MerchantQueryTools(merchantId, client, sqlValidator, bizJdbcTemplate);
        List<ToolCallback> tools = Arrays.asList(MethodToolCallbackProvider.builder()
                .toolObjects(queryTools).build().getToolCallbacks());
        List<Message> memory = List.of(new UserMessage(message));
        return new JChatMind(client, MERCHANT_SYSTEM_PROMPT, tools, sseService, sessionId, memory,
                agentProperties.getMaxIterations(), agentProperties.getTimeoutSeconds());
    }
}
