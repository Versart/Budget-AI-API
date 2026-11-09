package br.com.dio.budgetai.ai;

import br.com.dio.budgetai.domain.model.TransactionCategory;
import br.com.dio.budgetai.domain.model.TransactionType;
import br.com.dio.budgetai.dto.FinancialSummaryResponse;
import br.com.dio.budgetai.dto.TransactionRequest;
import br.com.dio.budgetai.dto.TransactionResponse;
import br.com.dio.budgetai.service.TransactionService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class TransactionTools {

    private final TransactionService transactionService;

    public TransactionTools(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Tool(description = "Registra uma nova transação financeira (receita INCOME ou despesa EXPENSE) no orçamento da pessoa usuária.")
    public TransactionResponse registerTransaction(
            @ToolParam(description = "Descrição curta da transação, ex: Supermercado, Salário, Uber") String description,
            @ToolParam(description = "Valor numérico positivo da transação, ex: 150.50") BigDecimal amount,
            @ToolParam(description = "Tipo da transação: INCOME para receita/entrada ou EXPENSE para despesa/gasto") String type,
            @ToolParam(description = "Categoria da transação: SALARY, FREELANCE, INVESTMENT, FOOD, HOUSING, TRANSPORT, HEALTH, EDUCATION, ENTERTAINMENT ou OTHER") String category,
            @ToolParam(description = "Data da transação no formato ISO YYYY-MM-DD (opcional, usa a data de hoje se omitido)") String dateIso
    ) {
        TransactionType parsedType = parseType(type);
        TransactionCategory parsedCategory = parseCategory(category);
        LocalDate parsedDate = (dateIso != null && !dateIso.isBlank())
                ? LocalDate.parse(dateIso.trim())
                : LocalDate.now();

        TransactionRequest request = new TransactionRequest(
                description,
                amount,
                parsedType,
                parsedCategory,
                parsedDate
        );
        return transactionService.create(request);
    }

    @Tool(description = "Consulta o resumo financeiro completo: total de receitas, total de despesas, saldo atual, limite mensal de gastos e gastos por categoria.")
    public FinancialSummaryResponse getFinancialSummary() {
        return transactionService.getFinancialSummary();
    }

    @Tool(description = "Lista todas as transações financeiras cadastradas ordenadas da mais recente para a mais antiga.")
    public List<TransactionResponse> listAllTransactions() {
        return transactionService.findAll();
    }

    @Tool(description = "Filtra e lista transações financeiras de uma categoria específica (FOOD, TRANSPORT, HOUSING, SALARY, etc.).")
    public List<TransactionResponse> listTransactionsByCategory(
            @ToolParam(description = "Categoria desejada: SALARY, FREELANCE, INVESTMENT, FOOD, HOUSING, TRANSPORT, HEALTH, EDUCATION, ENTERTAINMENT ou OTHER") String category
    ) {
        return transactionService.findByCategory(parseCategory(category));
    }

    @Tool(description = "Consulta transações financeiras realizadas dentro de um intervalo de datas (início e fim no formato YYYY-MM-DD).")
    public List<TransactionResponse> listTransactionsByPeriod(
            @ToolParam(description = "Data inicial no formato YYYY-MM-DD") String startDateIso,
            @ToolParam(description = "Data final no formato YYYY-MM-DD") String endDateIso
    ) {
        LocalDate start = LocalDate.parse(startDateIso.trim());
        LocalDate end = LocalDate.parse(endDateIso.trim());
        return transactionService.findByPeriod(start, end);
    }

    @Tool(description = "Remove/exclui uma transação financeira pelo seu identificador (ID).")
    public String deleteTransactionById(
            @ToolParam(description = "ID numérico da transação a ser removida") Long id
    ) {
        transactionService.delete(id);
        return "Transação #" + id + " removida com sucesso.";
    }

    private TransactionType parseType(String type) {
        if (type == null || type.isBlank()) {
            return TransactionType.EXPENSE;
        }
        String normalized = type.trim().toUpperCase();
        if (normalized.contains("INCOME") || normalized.contains("RECEITA") || normalized.contains("ENTRADA")) {
            return TransactionType.INCOME;
        }
        return TransactionType.EXPENSE;
    }

    private TransactionCategory parseCategory(String category) {
        if (category == null || category.isBlank()) {
            return TransactionCategory.OTHER;
        }
        try {
            return TransactionCategory.valueOf(category.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return TransactionCategory.OTHER;
        }
    }
}
