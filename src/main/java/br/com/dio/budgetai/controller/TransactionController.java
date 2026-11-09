package br.com.dio.budgetai.controller;

import br.com.dio.budgetai.domain.model.TransactionCategory;
import br.com.dio.budgetai.domain.model.TransactionType;
import br.com.dio.budgetai.dto.FinancialSummaryResponse;
import br.com.dio.budgetai.dto.TransactionRequest;
import br.com.dio.budgetai.dto.TransactionResponse;
import br.com.dio.budgetai.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transações Financeiras", description = "Gerenciamento direto de receitas, despesas e relatórios de orçamento")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @Operation(summary = "Cria uma nova transação financeira (receita ou despesa)")
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody TransactionRequest request) {
        TransactionResponse created = transactionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "Lista todas as transações financeiras cadastradas")
    public ResponseEntity<List<TransactionResponse>> findAll() {
        return ResponseEntity.ok(transactionService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma transação financeira pelo ID")
    public ResponseEntity<TransactionResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.findById(id));
    }

    @GetMapping("/summary")
    @Operation(summary = "Retorna o resumo financeiro completo (receitas, despesas, saldo, alerta de limite e gastos por categoria)")
    public ResponseEntity<FinancialSummaryResponse> getSummary() {
        return ResponseEntity.ok(transactionService.getFinancialSummary());
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Filtra transações por categoria")
    public ResponseEntity<List<TransactionResponse>> findByCategory(@PathVariable TransactionCategory category) {
        return ResponseEntity.ok(transactionService.findByCategory(category));
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Filtra transações por tipo (INCOME ou EXPENSE)")
    public ResponseEntity<List<TransactionResponse>> findByType(@PathVariable TransactionType type) {
        return ResponseEntity.ok(transactionService.findByType(type));
    }

    @GetMapping("/period")
    @Operation(summary = "Filtra transações dentro de um intervalo de datas")
    public ResponseEntity<List<TransactionResponse>> findByPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(transactionService.findByPeriod(startDate, endDate));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove uma transação financeira pelo ID")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        transactionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
