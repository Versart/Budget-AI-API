package br.com.dio.budgetai.domain.model;

public enum TransactionCategory {
    SALARY("Salário"),
    FREELANCE("Freelance"),
    INVESTMENT("Investimento"),
    FOOD("Alimentação"),
    HOUSING("Moradia"),
    TRANSPORT("Transporte"),
    HEALTH("Saúde"),
    EDUCATION("Educação"),
    ENTERTAINMENT("Lazer"),
    OTHER("Outros");

    private final String description;

    TransactionCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
