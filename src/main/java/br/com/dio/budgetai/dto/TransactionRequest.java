package br.com.dio.budgetai.dto;

import br.com.dio.budgetai.domain.model.TransactionCategory;
import br.com.dio.budgetai.domain.model.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(
        @NotBlank(message = "A descrição da transação é obrigatória")
        @Size(max = 150, message = "A descrição deve ter no máximo 150 caracteres")
        String description,

        @NotNull(message = "O valor da transação é obrigatório")
        @Positive(message = "O valor da transação deve ser maior que zero")
        BigDecimal amount,

        @NotNull(message = "O tipo da transação (INCOME ou EXPENSE) é obrigatório")
        TransactionType type,

        TransactionCategory category,

        LocalDate transactionDate
) {
}
