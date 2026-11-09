package br.com.dio.budgetai.dto;

import java.math.BigDecimal;
import java.util.Map;

public record FinancialSummaryResponse(
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal balance,
        long transactionCount,
        BigDecimal monthlyExpenseLimit,
        boolean limitExceeded,
        Map<String, BigDecimal> expensesByCategory
) {
}
