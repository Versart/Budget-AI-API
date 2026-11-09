package br.com.dio.budgetai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class BudgetAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(BudgetAiApplication.class, args);
    }
}
