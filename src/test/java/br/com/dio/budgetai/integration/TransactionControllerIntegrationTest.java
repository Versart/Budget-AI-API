package br.com.dio.budgetai.integration;

import br.com.dio.budgetai.domain.model.TransactionCategory;
import br.com.dio.budgetai.domain.model.TransactionType;
import br.com.dio.budgetai.domain.repository.TransactionRepository;
import br.com.dio.budgetai.dto.TransactionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/transactions deve criar transação e GET /api/transactions/summary deve retornar saldo atualizado")
    void shouldCreateTransactionsAndCalculateSummaryEndToEnd() throws Exception {
        TransactionRequest income = new TransactionRequest(
                "Salário DIO",
                new BigDecimal("5000.00"),
                TransactionType.INCOME,
                TransactionCategory.SALARY,
                LocalDate.of(2026, 11, 1)
        );

        TransactionRequest expense = new TransactionRequest(
                "Aluguel",
                new BigDecimal("1800.00"),
                TransactionType.EXPENSE,
                TransactionCategory.HOUSING,
                LocalDate.of(2026, 11, 5)
        );

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(income)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description", is("Salário DIO")))
                .andExpect(jsonPath("$.type", is("INCOME")));

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expense)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description", is("Aluguel")))
                .andExpect(jsonPath("$.type", is("EXPENSE")));

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get("/api/transactions/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome", is(5000.0)))
                .andExpect(jsonPath("$.totalExpense", is(1800.0)))
                .andExpect(jsonPath("$.balance", is(3200.0)))
                .andExpect(jsonPath("$.transactionCount", is(2)))
                .andExpect(jsonPath("$.limitExceeded", is(false)));
    }

    @Test
    @DisplayName("POST /api/transactions com dados inválidos deve retornar 400 Bad Request com ProblemDetail")
    void shouldReturnBadRequestWhenPayloadIsInvalid() throws Exception {
        TransactionRequest invalidRequest = new TransactionRequest(
                "",
                new BigDecimal("-50.00"),
                null,
                TransactionCategory.OTHER,
                LocalDate.now()
        );

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Erro de validação")));
    }

    @Test
    @DisplayName("DELETE /api/transactions/{id} inexistente deve retornar 404 Not Found")
    void shouldReturnNotFoundWhenDeletingNonExistentTransaction() throws Exception {
        mockMvc.perform(delete("/api/transactions/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Recurso não encontrado")));
    }
}
