package br.com.dio.budgetai.unit;

import br.com.dio.budgetai.ai.TransactionTools;
import br.com.dio.budgetai.domain.model.TransactionCategory;
import br.com.dio.budgetai.domain.model.TransactionType;
import br.com.dio.budgetai.dto.FinancialSummaryResponse;
import br.com.dio.budgetai.dto.TransactionRequest;
import br.com.dio.budgetai.dto.TransactionResponse;
import br.com.dio.budgetai.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionToolsTest {

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransactionTools transactionTools;

    @Test
    @DisplayName("Tool registerTransaction deve converter parâmetros da IA e acionar TransactionService")
    void shouldRegisterTransactionViaToolCalling() {
        TransactionResponse expected = new TransactionResponse(
                1L,
                "Jantar",
                new BigDecimal("120.00"),
                TransactionType.EXPENSE,
                TransactionCategory.FOOD,
                LocalDate.of(2026, 11, 8),
                LocalDateTime.now()
        );
        when(transactionService.create(any(TransactionRequest.class))).thenReturn(expected);

        TransactionResponse actual = transactionTools.registerTransaction(
                "Jantar",
                new BigDecimal("120.00"),
                "EXPENSE",
                "FOOD",
                "2026-11-08"
        );

        ArgumentCaptor<TransactionRequest> captor = ArgumentCaptor.forClass(TransactionRequest.class);
        verify(transactionService).create(captor.capture());

        TransactionRequest captured = captor.getValue();
        assertThat(captured.description()).isEqualTo("Jantar");
        assertThat(captured.amount()).isEqualByComparingTo("120.00");
        assertThat(captured.type()).isEqualTo(TransactionType.EXPENSE);
        assertThat(captured.category()).isEqualTo(TransactionCategory.FOOD);
        assertThat(captured.transactionDate()).isEqualTo(LocalDate.of(2026, 11, 8));
        assertThat(actual.id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Tool getFinancialSummary deve retornar o resumo financeiro do serviço")
    void shouldReturnFinancialSummaryViaToolCalling() {
        FinancialSummaryResponse expected = new FinancialSummaryResponse(
                new BigDecimal("5000.00"),
                new BigDecimal("1500.00"),
                new BigDecimal("3500.00"),
                4L,
                new BigDecimal("3000.00"),
                false,
                Map.of("FOOD", new BigDecimal("1500.00"))
        );
        when(transactionService.getFinancialSummary()).thenReturn(expected);

        FinancialSummaryResponse result = transactionTools.getFinancialSummary();

        assertThat(result.balance()).isEqualByComparingTo("3500.00");
        verify(transactionService).getFinancialSummary();
    }

    @Test
    @DisplayName("Tool listTransactionsByCategory deve converter categoria desconhecida para OTHER com segurança")
    void shouldFallbackToOtherCategoryWhenUnknownCategoryProvided() {
        when(transactionService.findByCategory(TransactionCategory.OTHER)).thenReturn(List.of());

        List<TransactionResponse> result = transactionTools.listTransactionsByCategory("CATEGORIA_INEXISTENTE");

        assertThat(result).isEmpty();
        verify(transactionService).findByCategory(TransactionCategory.OTHER);
    }

    @Test
    @DisplayName("Tool deleteTransactionById deve excluir transação e retornar confirmação textual")
    void shouldDeleteTransactionViaToolCalling() {
        String message = transactionTools.deleteTransactionById(42L);

        verify(transactionService).delete(42L);
        assertThat(message).contains("Transação #42 removida com sucesso");
    }
}
