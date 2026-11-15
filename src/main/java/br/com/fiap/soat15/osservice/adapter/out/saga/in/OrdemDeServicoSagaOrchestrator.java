package br.com.fiap.soat15.osservice.adapter.out.saga.in;

import br.com.fiap.soat15.osservice.adapter.out.saga.dto.BillingResponse;
import br.com.fiap.soat15.osservice.adapter.out.saga.dto.CancelarOsDto;
import br.com.fiap.soat15.osservice.adapter.out.saga.dto.CompensacaoDto;
import br.com.fiap.soat15.osservice.adapter.out.saga.dto.ExecucaoResultadoEventDto;
import br.com.fiap.soat15.osservice.adapter.out.saga.out.SqsBillingService;
import br.com.fiap.soat15.osservice.application.dto.AvancarStatusDTO;
import br.com.fiap.soat15.osservice.domain.entity.OrdemDeServico;
import br.com.fiap.soat15.osservice.domain.entity.StatusBilling;
import br.com.fiap.soat15.osservice.domain.entity.StatusOS;
import br.com.fiap.soat15.osservice.domain.entity.StatusSaga;
import br.com.fiap.soat15.osservice.domain.exception.BusinessException;
import br.com.fiap.soat15.osservice.domain.service.AgendaExecucaoService;
import br.com.fiap.soat15.osservice.domain.service.OrdemDeServicoService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class OrdemDeServicoSagaOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(OrdemDeServicoSagaOrchestrator.class);

    private final OrdemDeServicoService ordemService;
    private final SqsBillingService sqsBillingService;
    private final AgendaExecucaoService agendaExecucaoService;

    @Transactional
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
                    logger.info("SAGA [Sucesso]: Peças reservadas e execução agendada para a OS {}", osId);
                }
                case "ExecucaoConcluida" -> {
                    AvancarStatusDTO dto = AvancarStatusDTO.builder()
                            .novoStatus(StatusOS.CONCLUIDA)
                            .statusSaga(StatusSaga.AGUARDANDO_ENTREGA)
                            .build();
                    ordemService.avancarStatus(osId, dto);
                    logger.info("Trabalho concluído com sucesso na OS {}", osId);
                }
                case "ExecucaoRejeitada" -> {
                    CompensacaoDto dto = new CompensacaoDto(evento.envelopeResponseSQS().osId(), evento.envelopeResponseSQS().type());
                    dto.setEnvelopeResponseSQS(evento.envelopeResponseSQS());
                    acionarCompensacao(dto);
                    String motivo = evento.payload() != null ? evento.payload().motivo() : "DESCONHECIDO";
                    logger.warn("SAGA [Falha]: Rejeitado por {}. Acionando compensação para a OS {}", motivo, osId);
                }
                case "ExecucaoCancelada" -> {
                    CompensacaoDto dto = new CompensacaoDto(evento.envelopeResponseSQS().osId(), evento.envelopeResponseSQS().type());
                    dto.setEnvelopeResponseSQS(evento.envelopeResponseSQS());
                    acionarCompensacao(dto);
                    logger.info("Compensação efetivada: OS {} cancelada e estoque devolvido.", osId);
                }

                case "CancelamentoExecucaoRecusado" -> {
                    ordemService.atualizarStatusSaga(Long.parseLong(evento.envelopeResponseSQS().osId()), StatusSaga.FALHA_COMPENSACAO);
                    logger.info("Compensação não realizada: OS {} motivo: ", evento.payload().motivo());
                }

                default -> logger.warn("Evento desconhecido recebido: {}", tipoEvento);
            }
        } catch (Exception e) {
            logger.error("Erro crítico ao processar o evento da SAGA: {}", evento, e);
            throw new RuntimeException(e);
        }
    }

    @Transactional
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
                    final String type = "EstornarPagamento";
                    this.acionarCompensacao(new CompensacaoDto(evento.ordemId().toString(), type));
                }

                default -> logger.warn("Evento desconhecido recebido do serviço de pagamentos");
            }


        } catch (Exception e) {
            logger.error("Erro crítico ao processar o evento da SAGA: {}", evento, e);
            throw new RuntimeException(e);
        }

    }

    @Transactional()
    public void acionarCompensacao(CompensacaoDto compensacaoDto) {
        OrdemDeServico ordem = ordemService.buscarEntidade(Long.parseLong(compensacaoDto.getOsId()));

        final String causationId = "evento-cancelar-os-" + ordem.getId();

        CancelarOsDto dto = new CancelarOsDto(ordem, compensacaoDto.getType(), causationId);

        StatusOS statusOS = ordem.getStatus();

        if (statusOS.equals(StatusOS.APROVADA) || statusOS.equals(StatusOS.EM_EXECUCAO))
            this.estornoPagamento(dto);

        else
            throw new BusinessException("Não é possível cancelar a OS no status atual: " + statusOS);

    }

    @Transactional
    private void estornoPagamento(CancelarOsDto dto) {
        sqsBillingService.enviarComandoProcessarPagamento(dto);
        this.cancelarOS(dto);
    }

    @Transactional
    private void cancelarOS(CancelarOsDto dto) {
        ordemService.avancarStatus(
                dto.getOrdemDeServico().getId(),
                AvancarStatusDTO.builder()
                        .novoStatus(StatusOS.CANCELADA)
                        .statusSaga(StatusSaga.COMPENSADA)
                        .build());
    }

}