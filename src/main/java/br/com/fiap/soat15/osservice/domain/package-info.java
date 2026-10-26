/**
 * Camada de dominio do OS Service.
 *
 * TODO: este servico vai absorver boa parte do dominio atual do monolito (pacote
 * domain.entity do projeto principal):
 *  - Cliente, Veiculo, OrdemDeServico, StatusOS.
 *
 * Alem disso, este servico hospeda o orquestrador do Saga Pattern que coordena o fluxo
 * de abertura/orcamento/pagamento/execucao entre os 3 microsservicos (ver
 * documentacao/event-storming-saga-v1.md no repositorio principal do grupo):
 *  - Comandos enviados via SQS: GerarOrcamento (Billing), ProcessarPagamento (Billing),
 *    EnviarParaExecucao (Execucao).
 *  - Eventos consumidos via SQS: OrcamentoGerado, PagamentoConfirmado, PagamentoRecusado,
 *    ExecucaoConcluida, ExecucaoFalhou.
 *  - Compensacoes: CancelarOrdemDeServico, EstornarPagamento (ver tabela de compensacoes
 *    no event storming).
 *
 * Persistencia via JPA/PostgreSQL (RDS compartilhado com o Billing Service, databases e
 * usuarios separados - ver ADR-P2). Nenhuma entidade de dominio real foi criada ainda;
 * isso fica para quem assumir a implementacao, seguindo o planejamento em
 * documentacao/divisao-tarefas-fase4.md no repositorio principal do grupo.
 */
package br.com.fiap.soat15.osservice.domain;
