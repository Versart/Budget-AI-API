package br.com.dio.budgetai.service;

import br.com.dio.budgetai.domain.model.Transaction;
import br.com.dio.budgetai.domain.model.TransactionCategory;
import br.com.dio.budgetai.domain.model.TransactionType;
import br.com.dio.budgetai.domain.repository.TransactionRepository;
import br.com.dio.budgetai.dto.FinancialSummaryResponse;
import br.com.dio.budgetai.dto.TransactionRequest;
import br.com.dio.budgetai.dto.TransactionResponse;
import br.com.dio.budgetai.exception.BusinessException;
import br.com.dio.budgetai.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final BigDecimal monthlyExpenseLimit;

    public TransactionService(
            TransactionRepository transactionRepository,
            @Value("${budget.monthly-expense-limit:5000.00}") BigDecimal monthlyExpenseLimit
    ) {
        this.transactionRepository = transactionRepository;
        this.monthlyExpenseLimit = monthlyExpenseLimit;
    }

    @Transactional
    public TransactionResponse create(TransactionRequest request) {
        validateTransactionRequest(request);

        Transaction transaction = new Transaction(
                request.description().trim(),
                request.amount(),
                request.type(),
                request.category() != null ? request.category() : TransactionCategory.OTHER,
                request.transactionDate() != null ? request.transactionDate() : LocalDate.now()
        );

        Transaction saved = transactionRepository.save(transaction);
        return TransactionResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> findAll() {
        return transactionRepository.findAll(Sort.by(Sort.Direction.DESC, "transactionDate", "id"))
                .stream()
                .map(TransactionResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransactionResponse findById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada com ID: " + id));
        return TransactionResponse.fromEntity(transaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> findByCategory(TransactionCategory category) {
        return transactionRepository.findByCategoryOrderByTransactionDateDesc(category)
                .stream()
                .map(TransactionResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> findByType(TransactionType type) {
        return transactionRepository.findByTypeOrderByTransactionDateDesc(type)
                .stream()
                .map(TransactionResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> findByPeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BusinessException("As datas inicial e final são obrigatórias para consulta por período");
        }
        if (startDate.isAfter(endDate)) {
            throw new BusinessException("A data inicial não pode ser posterior à data final");
        }
        return transactionRepository.findByTransactionDateBetweenOrderByTransactionDateDesc(startDate, endDate)
                .stream()
                .map(TransactionResponse::fromEntity)
                .toList();
    }

    @Transactional
    public void delete(Long id) {
        if (!transactionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Transação não encontrada com ID: " + id);
        }
        transactionRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public FinancialSummaryResponse getFinancialSummary() {
        BigDecimal totalIncome = transactionRepository.sumAmountByType(TransactionType.INCOME);
        BigDecimal totalExpense = transactionRepository.sumAmountByType(TransactionType.EXPENSE);
        BigDecimal balance = totalIncome.subtract(totalExpense);
        long count = transactionRepository.count();

        List<Transaction> expenses = transactionRepository.findByTypeOrderByTransactionDateDesc(TransactionType.EXPENSE);
        Map<String, BigDecimal> expensesByCategory = expenses.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getCategory().name(),
                        LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        boolean limitExceeded = totalExpense.compareTo(monthlyExpenseLimit) > 0;

        return new FinancialSummaryResponse(
                totalIncome,
                totalExpense,
                balance,
                count,
                monthlyExpenseLimit,
                limitExceeded,
                expensesByCategory
        );
    }

    private void validateTransactionRequest(TransactionRequest request) {
        if (request == null) {
            throw new BusinessException("Os dados da transação não podem ser nulos");
        }
        if (request.description() == null || request.description().isBlank()) {
            throw new BusinessException("A descrição da transação é obrigatória");
        }
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("O valor da transação deve ser maior que zero");
        }
        if (request.type() == null) {
            throw new BusinessException("O tipo da transação (INCOME ou EXPENSE) é obrigatório");
        }
    }
}
