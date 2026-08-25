package com.savory.ai.dto;

/**
 * Agent SSE 事件负载
 *
 * @param type    事件类型: thinking / action / message / done
 * @param content 事件内容（message 为合并后的文本，done 为空）
 */
public record AgentEvent(String type, String content) {
}
