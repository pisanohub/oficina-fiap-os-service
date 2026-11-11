package br.com.fiap.soat15.osservice.adapter.out.saga.in;

import br.com.fiap.soat15.osservice.adapter.out.persistence.repository.SagaHistoricoRepository;
import br.com.fiap.soat15.osservice.adapter.out.saga.dto.BillingResponse;
import br.com.fiap.soat15.osservice.adapter.out.saga.dto.EnvelopeResponseSQS;
import br.com.fiap.soat15.osservice.adapter.out.saga.dto.ExecucaoResultadoEventDto;
import br.com.fiap.soat15.osservice.adapter.out.saga.out.SqsExecucaoService;
import br.com.fiap.soat15.osservice.application.dto.AvancarStatusDTO;
import br.com.fiap.soat15.osservice.domain.entity.SagaHistorico;
import br.com.fiap.soat15.osservice.domain.entity.StatusBilling;
import br.com.fiap.soat15.osservice.domain.entity.StatusOS;
import br.com.fiap.soat15.osservice.domain.entity.StatusSaga;
import br.com.fiap.soat15.osservice.domain.service.AgendaExecucaoService;
import br.com.fiap.soat15.osservice.domain.service.OrdemDeServicoService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@RequiredArgsConstructor
@Service
public class OrdemDeServicoSagaOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(OrdemDeServicoSagaOrchestrator.class);

    private final OrdemDeServicoService ordemService;
    private final SqsExecucaoService sqsExecucaoService;
    private final SagaHistoricoRepository sagaHistoricoRepository;
    private final AgendaExecucaoService agendaExecucaoService;

    public void processarRespostaExecucao(ExecucaoResultadoEventDto evento) {
        try {
            String tipoEvento = evento.envelopeResponseSQS().type();
            String osIdStr = evento.envelopeResponseSQS().osId();
            Long osId = Long.parseLong(osIdStr);

            switch (tipoEvento) {
                case "ExecucaoAgendada" -> {
                    AvancarStatusDTO dto = AvancarStatusDTO.builder()
                            .novoStatus(StatusOS.EM_EXECUCAO)
                            .build();
                    ordemService.avancarStatus(osId, dto);
                    salvarHistorico(evento.envelopeResponseSQS());
                    logger.info("SAGA [Sucesso]: Peças reservadas e execução agendada para a OS {}", osId);
                }
                case "ExecucaoConcluida" -> {
                    AvancarStatusDTO dto = AvancarStatusDTO.builder()
                            .novoStatus(StatusOS.CONCLUIDA)
                            .statusSaga(StatusSaga.AGUARDANDO_ENTREGA)
                            .build();
                    ordemService.avancarStatus(osId, dto);
                    salvarHistorico(evento.envelopeResponseSQS());
                    logger.info("Trabalho concluído com sucesso na OS {}", osId);
                }
                case "ExecucaoRejeitada" -> {
                    acionarCompensacao(evento);
                    String motivo = evento.payload() != null ? evento.payload().motivo() : "DESCONHECIDO";
                    logger.warn("SAGA [Falha]: Rejeitado por {}. Acionando compensação para a OS {}", motivo, osId);
                }
                case "ExecucaoCancelada" -> {
                    acionarCompensacao(evento);
                    logger.info("Compensação efetivada: OS {} cancelada e estoque devolvido.", osId);
                }
                default -> logger.warn("Evento desconhecido recebido: {}", tipoEvento);
            }
        } catch (Exception e) {
            logger.error("Erro crítico ao processar o evento da SAGA: {}", evento, e);
            throw new RuntimeException(e);
        }
    }


    public void processarRespostaBilling(BillingResponse.MessageDto evento) {
        try {

            StatusBilling status = evento.status();
            Long osId = evento.ordemId();

            switch (status) {
                case StatusBilling.AGUARDANDO_APROVACAO -> {
                    BillingResponse.OrcamentoResponseDto orcamento =
                            (BillingResponse.OrcamentoResponseDto) evento;
                    AvancarStatusDTO dto = AvancarStatusDTO.builder()
                            .novoStatus(StatusOS.AGUARDANDO_APROVACAO)
                            .observacoes("Orçamento criado com sucesso.")
                            .total(orcamento.total())
                            .orcamentoId(orcamento.id())
                            .build();
                    ordemService.avancarStatus(osId, dto);
                    logger.info("SAGA [Sucesso]: Orçamento gerado com sucesso para OS {}", osId);
                }

                case StatusBilling.APROVADO -> {
                    agendaExecucaoService.executar((BillingResponse.PagamentoResponseDto) evento);
                    logger.info("SAGA [Sucesso]: Pagamento realizado com sucesso para OS {}", osId);
                }

                case StatusBilling.FALHA -> {
                /// VALIDAR ESTORNO
                }

                default -> logger.warn("Evento desconhecido recebido do serviço de pagamentos");
            }


        } catch (Exception e) {
            logger.error("Erro crítico ao processar o evento da SAGA: {}", evento, e);
            throw new RuntimeException(e);
        }

    }


    private void acionarCompensacao(ExecucaoResultadoEventDto evento) {
        String osId = evento.envelopeResponseSQS().osId();
        String sagaId = evento.envelopeResponseSQS().sagaId();
        String causationId = evento.envelopeResponseSQS().causationId();
        sqsExecucaoService.enviarComandoCancelamento(osId, sagaId, causationId);

        // Lógica de Rollback:
        // 1. Chamar o serviço de Billing para solicitar o estorno do pagamento (pagamentoId).
        /// pendente

        // 2. Atualizar o status da OS no banco de dados para FALHA_SAGA ou CANCELADA.
        AvancarStatusDTO dto = AvancarStatusDTO.builder()
                .novoStatus(StatusOS.CANCELADA)
                .observacoes(evento.payload().motivo())
                .build();

        ordemService.avancarStatus(Long.getLong(osId), dto);
        salvarHistorico(evento.envelopeResponseSQS());
    }

    private void salvarHistorico(EnvelopeResponseSQS envelopeSqs) {
        SagaHistorico historico = new SagaHistorico();
        historico.setSagaId(envelopeSqs.sagaId());
        historico.setOsId(envelopeSqs.osId());
        historico.setMessageId(envelopeSqs.messageId());
        historico.setType(envelopeSqs.type());
        historico.setOperationId(envelopeSqs.operationId());
        historico.setOccurredAt(Instant.now());

        sagaHistoricoRepository.save(historico);
    }

}