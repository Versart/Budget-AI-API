# 💰 Budget AI API — Assistente Financeiro Inteligente com Spring Boot & Spring AI

API REST inteligente de controle de orçamento pessoal desenvolvida com **Spring Boot 3** e **Spring AI**, capaz de processar comandos em **voz (áudio)** e **texto**, interpretar a intenção da pessoa usuária com Modelos de Linguagem (LLMs), executar funções reais de negócio via **Tool Calling (Function Calling)**, persistir transações financeiras e responder tanto em texto estruturado quanto em voz sintetizada (**Text-to-Speech**).

---

## 📌 1. O Que o Projeto Faz

A **Budget AI API** conecta Inteligência Artificial Generativa a uma aplicação Java corporativa real de gestão financeira pessoal. O fluxo principal da aplicação realiza as seguintes etapas:

1. **Recebimento do comando**: Aceita um arquivo de áudio (`multipart/form-data`) ou um comando em texto JSON enviado pelo cliente.
2. **Transcrição de Áudio em Texto (Speech-to-Text)**: Converte o áudio enviado pelo cliente em texto utilizando o modelo `whisper-1` via `TranscriptionModel` do Spring AI.
3. **Interpretação de Intenção com IA (`ChatClient`)**: O `ChatClient` analisa o comando com base em um *System Prompt* especializado em finanças pessoais.
4. **Execução de Funções Reais (`@Tool` / Tool Calling)**: O modelo decide autonomamente qual ferramenta Java invocar (`registerTransaction`, `getFinancialSummary`, `listAllTransactions`, `listTransactionsByCategory`, `listTransactionsByPeriod`, `deleteTransactionById`).
5. **Persistência e Auditoria**: Salva ou consulta as transações financeiras no banco de dados relacional via Spring Data JPA e registra o histórico de auditoria (`AuditLog` e `@CreatedDate` / `@LastModifiedDate`).
6. **Geração da Resposta Final**: Retorna a resposta amigável ao cliente em JSON ou sintetiza a resposta em áudio MP3 (**Text-to-Speech** via `tts-1` / `SpeechModel`).

---

## 🛠️ 2. Tecnologias Utilizadas

- **Java 21**
- **Spring Boot 3.4.3** (`spring-boot-starter-web`, `spring-boot-starter-validation`, `spring-boot-starter-data-jpa`)
- **Spring AI 1.0.0-M6** (`spring-ai-openai-spring-boot-starter` — `ChatClient`, `@Tool`, `TranscriptionModel`, `SpeechModel`)
- **Banco de Dados H2** (em memória para execução imediata e testes automatizados)
- **SpringDoc OpenAPI / Swagger UI** (documentação interativa da API REST)
- **JUnit 5, Mockito, AssertJ & Spring MockMvc** (Testes de Unidade e de Integração)

---

## 🚀 3. Melhorias e Evoluções Implementadas

Além do fluxo base proposto nas aulas do expert Thiago Poiani (`05-spring-ai`), foram implementadas as seguintes evoluções:

1. **Novas Ferramentas para o Tool Calling (`TransactionTools`)**:
   - Registro inteligente de receitas (`INCOME`) e despesas (`EXPENSE`) com classificação automática de categorias (`SALARY`, `FREELANCE`, `INVESTMENT`, `FOOD`, `HOUSING`, `TRANSPORT`, `HEALTH`, `EDUCATION`, `ENTERTAINMENT`, `OTHER`);
   - Consulta de resumo financeiro consolidado (`getFinancialSummary`) com saldo, total de entradas/saídas, detalhamento de despesas por categoria e alerta de limite mensal excedido;
   - Filtro de transações por categoria (`listTransactionsByCategory`) e por intervalo de datas (`listTransactionsByPeriod`);
   - Exclusão de transações por ID (`deleteTransactionById`).
2. **Validações de Dados e Tratamento Global de Erros (`GlobalExceptionHandler`)**:
   - Validação com Bean Validation (`@NotBlank`, `@NotNull`, `@Positive`, `@Size`) nos DTOs e validação de regras de negócio no `TransactionService`, retornando respostas padronizadas no formato RFC 7807 (`ProblemDetail`).
3. **Auditoria Completa da API**:
   - JPA Auditing (`@EnableJpaAuditing`, `@CreatedDate`, `@LastModifiedDate`) na entidade `Transaction`;
   - Registro de todas as interações com a IA na entidade `AuditLog` (`tb_ai_audit_logs`), consultável pelo endpoint `GET /api/assistant/audit`.
4. **Suíte Completa de Testes de Unidade e Integração**:
   - **Testes de Unidade**: `TransactionServiceTest`, `TransactionToolsTest`, `AudioServiceTest` e `AssistantServiceTest`.
   - **Testes de Integração**: `TransactionRepositoryIntegrationTest` (`@DataJpaTest`), `TransactionControllerIntegrationTest` (`@SpringBootTest` + `MockMvc`) e `AssistantControllerIntegrationTest` (fluxo completo de voz, texto, Tool Calling, TTS e auditoria com `@SpringBootTest`).

---

## ⚙️ 4. Como Executar a Aplicação

### Pré-requisitos
- **JDK 21** instalado
- **Maven 3.9+** instalado
- Uma chave de API da OpenAI configurada na variável de ambiente `OPENAI_API_KEY` (necessária apenas para chamadas reais à OpenAI; os testes automatizados funcionam 100% offline).

### Passo a passo

1. Configure sua chave da OpenAI no terminal:
   ```powershell
   $env:OPENAI_API_KEY="sk-sua-chave-aqui"
   ```

2. Execute a aplicação com o Maven:
   ```powershell
   mvn spring-boot:run
   ```

3. Acesse a documentação interativa **Swagger UI**:
   - `http://localhost:8080/swagger-ui.html`
   - Console do banco H2: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:budgetdb`)

---

## 🧪 5. Como Testar o Fluxo Principal

### 5.1. Executando os Testes Automatizados (Unidade e Integração)
Para rodar toda a suíte de testes unitários e de integração:
```powershell
mvn clean test
```

### 5.2. Exemplos de Requisições REST (`curl`)

#### 1) Enviar comando por Voz (Áudio -> Texto -> IA Tool Calling -> Resposta JSON)
```bash
curl -X POST http://localhost:8080/api/assistant/voice \
  -F "audio=@comando-gasto-mercado.mp3"
```
**Exemplo de Resposta:**
```json
{
  "channel": "VOICE",
  "recognizedCommand": "Registre uma despesa de 150 reais no supermercado hoje",
  "assistantReply": "Registrei sua despesa de R$ 150,00 na categoria Alimentação (Supermercado) para a data de hoje!",
  "processedAt": "2026-11-08T18:20:00"
}
```

#### 2) Enviar comando por Voz e receber resposta falada em MP3 (Voice-to-Voice)
```bash
curl -X POST http://localhost:8080/api/assistant/voice/audio-reply \
  -F "audio=@pergunta-saldo.mp3" \
  --output resposta-assistente.mp3
```

#### 3) Enviar comando por Texto para o Assistente IA
```bash
curl -X POST http://localhost:8080/api/assistant/text \
  -H "Content-Type: application/json" \
  -d '{"command": "Recebi 5000 reais de salário e gastei 180 reais de combustível. Qual é o meu saldo?"}'
```

#### 4) Consultar o Resumo Financeiro diretamente via REST
```bash
curl -X GET http://localhost:8080/api/transactions/summary
```
**Exemplo de Resposta:**
```json
{
  "totalIncome": 5000.00,
  "totalExpense": 330.00,
  "balance": 4670.00,
  "transactionCount": 3,
  "monthlyExpenseLimit": 5000.00,
  "limitExceeded": false,
  "expensesByCategory": {
    "FOOD": 150.00,
    "TRANSPORT": 180.00
  }
}
```

#### 5) Consultar o Histórico de Auditoria da IA
```bash
curl -X GET http://localhost:8080/api/assistant/audit
```

---
