# OS Service

Microsservico de Ordem de Servico da Oficina FIAP (Tech Challenge Fase 4).

## Responsabilidade

Gerencia o ciclo de vida da Ordem de Servico - abertura, atualizacao de status, consulta
de status e historico. Tambem hospeda o orquestrador do Saga Pattern que coordena o fluxo
entre os 3 microsservicos.

## Tecnologias

- Java 21
- Spring Boot 4.0.5
  - Spring Web, Data JPA, Validation, Actuator
- PostgreSQL (driver JDBC) - RDS compartilhado com o Billing Service, database e usuario
  proprios (ver ADR-P2)
- AWS SDK v2
  - SQS (comandos/eventos da Saga)
- Resilience4j (Circuit Breaker nas chamadas sincronas)
- springdoc-openapi (Swagger UI)
- Lombok
- Maven + Maven Wrapper
- JaCoCo (cobertura de testes)

## Como rodar localmente

Pre-requisitos: Java 21 e um PostgreSQL acessivel (local ou via Docker).

```bash
./mvnw clean verify
./mvnw spring-boot:run
```

No Windows (PowerShell/cmd):

```
mvnw.cmd clean verify
mvnw.cmd spring-boot:run
```

Variaveis de ambiente configuraveis (ver `src/main/resources/application.yaml`):

| Variavel | Default | Descricao |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/os_service` | URL JDBC do PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | Usuario do banco |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | Senha do banco |
| `AWS_REGION` | `us-east-1` | Regiao AWS usada pelo cliente SQS |

Health check: `GET /actuator/health`.

## Observacao importante

Este repositorio e um **esqueleto inicial**, criado apenas para dar um ponto de partida
(build funcionando, estrutura de pastas, Dockerfile e manifestos Kubernetes basicos,
pipeline de CI simples). Nenhuma logica de dominio foi implementada ainda.

Quem assumir este servico deve seguir o planejamento completo em
`documentacao/divisao-tarefas-fase4.md` e `documentacao/event-storming-saga-v1.md`, no
repositorio principal do grupo, incluindo:

- Modelagem do dominio (Cliente, Veiculo, OrdemDeServico, StatusOS, hoje no monolito).
- Implementacao do orquestrador da Saga: comandos `GerarOrcamento`, `ProcessarPagamento`
  e `EnviarParaExecucao` via SQS; consumo dos eventos de resposta (`OrcamentoGerado`,
  `PagamentoConfirmado`, `ExecucaoConcluida`) e das compensacoes de falha.
- Definicao de onde mora o estado da Saga (campo na propria OS ou tabela dedicada - ver
  hot spot no event storming).
- Circuit Breaker nas chamadas sincronas (ex.: API Gateway -> OS Service).
- Testes unitarios, cobertura minima de 80%, fluxo BDD.
- Pipeline de deploy em Kubernetes (este CI ainda so faz build + testes).
