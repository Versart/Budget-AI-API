package br.com.dio.budgetai.dto;

import jakarta.validation.constraints.NotBlank;

public record AssistantTextRequest(
        @NotBlank(message = "O comando para o assistente não pode estar vazio")
        String command
) {
}
