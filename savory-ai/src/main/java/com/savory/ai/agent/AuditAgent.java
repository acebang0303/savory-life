package com.savory.ai.agent;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 内容审核Agent
 * 对用户发布的内容（评价、笔记）进行AI审核
 *
 * 输出JSON格式: {"approved": true/false, "reason": "...", "riskLevel": "safe/low/medium/high"}
 */
@Component
@Slf4j
public class AuditAgent {

    @Autowired
    private ChatModel chatModel;

    private static final String AUDIT_PROMPT = """
            你是内容审核助手，负责审核用户提交的评价和笔记内容。

            审核规则：
            1. 禁止内容: 政治敏感、色情、暴力、赌博、毒品相关
            2. 风险内容: 人身攻击、恶意差评、虚假宣传、广告引流
            3. 低质量内容: 纯表情/无意义文字、明显复制粘贴

            请以JSON格式返回审核结果：
            {
                "approved": true/false,
                "reason": "通过原因或驳回原因",
                "riskLevel": "safe/low/medium/high"
            }

            只返回JSON，不要有任何其他内容。
            """;

    /**
     * 审核内容
     *
     * @param content 待审核的文本内容
     * @param contentType 内容类型: review/note
     * @return 审核结果JSON
     */
    public JSONObject audit(String content, String contentType) {
        log.info("内容审核Agent处理: type={}, content长度={}", contentType,
                content != null ? content.length() : 0);

        String prompt = String.format("请审核以下%s内容:\n\n%s", contentType, content);

        ChatClient client = ChatClient.builder(chatModel).build();
        String response = client.prompt()
                .system(AUDIT_PROMPT)
                .user(prompt)
                .call()
                .content();

        //解析JSON结果
        if (response != null) {
            try {
                return JSON.parseObject(response.trim());
            } catch (Exception e) {
                log.error("审核结果解析失败，默认放行: {}", e.getMessage());
                JSONObject fallback = new JSONObject();
                fallback.put("approved", true);
                fallback.put("reason", "AI审核结果解析异常，默认放行");
                fallback.put("riskLevel", "low");
                return fallback;
            }
        }

        //兜底: 放行
        JSONObject fallback = new JSONObject();
        fallback.put("approved", true);
        fallback.put("reason", "AI未返回结果，默认放行");
        fallback.put("riskLevel", "low");
        return fallback;
    }
}
