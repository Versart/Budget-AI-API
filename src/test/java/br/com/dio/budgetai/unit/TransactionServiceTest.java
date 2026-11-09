package br.com.dio.budgetai.unit;

import br.com.dio.budgetai.domain.model.Transaction;
import br.com.dio.budgetai.domain.model.TransactionCategory;
import br.com.dio.budgetai.domain.model.TransactionType;
import br.com.dio.budgetai.domain.repository.TransactionRepository;
import br.com.dio.budgetai.dto.FinancialSummaryResponse;
import br.com.dio.budgetai.dto.TransactionRequest;
import br.com.dio.budgetai.dto.TransactionResponse;
import br.com.dio.budgetai.exception.BusinessException;
import br.com.dio.budgetai.exception.ResourceNotFoundException;
import br.com.dio.budgetai.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(transactionRepository, new BigDecimal("3000.00"));
    }

    @Test
    @DisplayName("Deve criar uma transação válida com sucesso")
    void shouldCreateValidTransaction() {
        TransactionRequest request = new TransactionRequest(
                "Supermercado Mensal",
                new BigDecimal("450.00"),
                TransactionType.EXPENSE,
                TransactionCategory.FOOD,
                LocalDate.of(2026, 11, 8)
        );

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        TransactionResponse response = transactionService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.description()).isEqualTo("Supermercado Mensal");
        assertThat(response.amount()).isEqualByComparingTo("450.00");
        assertThat(response.type()).isEqualTo(TransactionType.EXPENSE);
        assertThat(response.category()).isEqualTo(TransactionCategory.FOOD);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Deve lançar BusinessException ao criar transação com valor negativo ou zero")
    void shouldThrowExceptionWhenAmountIsInvalid() {
        TransactionRequest request = new TransactionRequest(
                "Valor Inválido",
                BigDecimal.ZERO,
                TransactionType.EXPENSE,
                TransactionCategory.OTHER,
                LocalDate.now()
        );

        assertThatThrownBy(() -> transactionService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("maior que zero");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar BusinessException ao criar transação sem descrição")
    void shouldThrowExceptionWhenDescriptionIsBlank() {
        TransactionRequest request = new TransactionRequest(
                "   ",
                new BigDecimal("100.00"),
                TransactionType.INCOME,
                TransactionCategory.SALARY,
                LocalDate.now()
        );

        assertThatThrownBy(() -> transactionService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("descrição da transação é obrigatória");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve buscar transação por ID existente")
    void shouldFindTransactionById() {
        Transaction transaction = new Transaction(
                "Salário",
                new BigDecimal("5000.00"),
                TransactionType.INCOME,
                TransactionCategory.SALARY,
                LocalDate.of(2026, 11, 5)
        );
        transaction.setId(10L);

        when(transactionRepository.findById(10L)).thenReturn(Optional.of(transaction));

        TransactionResponse response = transactionService.findById(10L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.description()).isEqualTo("Salário");
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao buscar ID inexistente")
    void shouldThrowNotFoundWhenIdDoesNotExist() {
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Deve lançar BusinessException quando data inicial for posterior à data final na busca por período")
    void shouldThrowExceptionWhenPeriodIsInvalid() {
        LocalDate start = LocalDate.of(2026, 11, 10);
        LocalDate end = LocalDate.of(2026, 11, 1);

        assertThatThrownBy(() -> transactionService.findByPeriod(start, end))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("data inicial não pode ser posterior");
    }

    @Test
    @DisplayName("Deve calcular resumo financeiro corretamente e indicar quando limite mensal foi excedido")
    void shouldCalculateFinancialSummaryAndDetectLimitExceeded() {
        when(transactionRepository.sumAmountByType(TransactionType.INCOME))
                .thenReturn(new BigDecimal("6000.00"));
        when(transactionRepository.sumAmountByType(TransactionType.EXPENSE))
                .thenReturn(new BigDecimal("3500.00"));
        when(transactionRepository.count()).thenReturn(3L);

        Transaction exp1 = new Transaction("Aluguel", new BigDecimal("2500.00"),
                TransactionType.EXPENSE, TransactionCategory.HOUSING, LocalDate.now());
        Transaction exp2 = new Transaction("Mercado", new BigDecimal("1000.00"),
                TransactionType.EXPENSE, TransactionCategory.FOOD, LocalDate.now());

        when(transactionRepository.findByTypeOrderByTransactionDateDesc(TransactionType.EXPENSE))
                .thenReturn(List.of(exp1, exp2));

        FinancialSummaryResponse summary = transactionService.getFinancialSummary();

        assertThat(summary.totalIncome()).isEqualByComparingTo("6000.00");
        assertThat(summary.totalExpense()).isEqualByComparingTo("3500.00");
        assertThat(summary.balance()).isEqualByComparingTo("2500.00");
        assertThat(summary.transactionCount()).isEqualTo(3L);
        assertThat(summary.limitExceeded()).isTrue();
        assertThat(summary.expensesByCategory())
                .containsEntry("HOUSING", new BigDecimal("2500.00"))
                .containsEntry("FOOD", new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("Deve remover transação existente com sucesso")
    void shouldDeleteExistingTransaction() {
        when(transactionRepository.existsById(5L)).thenReturn(true);

        transactionService.delete(5L);

        verify(transactionRepository).deleteById(5L);
    }
}
