package com.savory.ai.dto;

import java.util.List;

public record AgentChatResponse(String conversationId, List<AgentEvent> events, String finalAnswer) {
}
