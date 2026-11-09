package br.com.dio.budgetai.dto;

import java.time.LocalDateTime;

public record AssistantResponse(
        String channel,
        String recognizedCommand,
        String assistantReply,
        LocalDateTime processedAt
) {
}
