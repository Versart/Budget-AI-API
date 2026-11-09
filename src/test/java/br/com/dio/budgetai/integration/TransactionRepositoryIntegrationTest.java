package br.com.dio.budgetai.integration;

import br.com.dio.budgetai.domain.model.Transaction;
import br.com.dio.budgetai.domain.model.TransactionCategory;
import br.com.dio.budgetai.domain.model.TransactionType;
import br.com.dio.budgetai.domain.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class TransactionRepositoryIntegrationTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();

        Transaction salary = new Transaction(
                "Salário Mensal",
                new BigDecimal("6000.00"),
                TransactionType.INCOME,
                TransactionCategory.SALARY,
                LocalDate.of(2026, 11, 1)
        );
        Transaction food = new Transaction(
                "Supermercado",
                new BigDecimal("850.50"),
                TransactionType.EXPENSE,
                TransactionCategory.FOOD,
                LocalDate.of(2026, 11, 5)
        );
        Transaction transport = new Transaction(
                "Combustível",
                new BigDecimal("250.00"),
                TransactionType.EXPENSE,
                TransactionCategory.TRANSPORT,
                LocalDate.of(2026, 11, 7)
        );

        transactionRepository.saveAll(List.of(salary, food, transport));
    }

    @Test
    @DisplayName("Deve somar corretamente os valores por tipo de transação via query JPQL")
    void shouldSumAmountsByType() {
        BigDecimal totalIncome = transactionRepository.sumAmountByType(TransactionType.INCOME);
        BigDecimal totalExpense = transactionRepository.sumAmountByType(TransactionType.EXPENSE);

        assertThat(totalIncome).isEqualByComparingTo("6000.00");
        assertThat(totalExpense).isEqualByComparingTo("1100.50");
    }

    @Test
    @DisplayName("Deve buscar transações filtradas por categoria")
    void shouldFindTransactionsByCategory() {
        List<Transaction> foodTransactions = transactionRepository
                .findByCategoryOrderByTransactionDateDesc(TransactionCategory.FOOD);

        assertThat(foodTransactions).hasSize(1);
        assertThat(foodTransactions.get(0).getDescription()).isEqualTo("Supermercado");
    }

    @Test
    @DisplayName("Deve buscar transações dentro de um período específico")
    void shouldFindTransactionsBetweenDates() {
        List<Transaction> periodTransactions = transactionRepository
                .findByTransactionDateBetweenOrderByTransactionDateDesc(
                        LocalDate.of(2026, 11, 4),
                        LocalDate.of(2026, 11, 8)
                );

        assertThat(periodTransactions).hasSize(2);
        assertThat(periodTransactions.get(0).getDescription()).isEqualTo("Combustível");
        assertThat(periodTransactions.get(1).getDescription()).isEqualTo("Supermercado");
    }
}
