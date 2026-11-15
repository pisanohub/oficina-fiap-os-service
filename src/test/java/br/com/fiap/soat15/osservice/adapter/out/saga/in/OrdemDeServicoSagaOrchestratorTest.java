
package br.com.fiap.soat15.osservice.adapter.out.saga.in;

import br.com.fiap.soat15.osservice.adapter.out.saga.dto.BillingResponse;
import br.com.fiap.soat15.osservice.adapter.out.saga.dto.CompensacaoDto;
import br.com.fiap.soat15.osservice.adapter.out.saga.dto.EnvelopeResponseSQS;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdemDeServicoSagaOrchestratorTest {

    private static final Long OS_ID = 10L;
    private static final Long CLIENTE_ID = 20L;

    @Mock
    private OrdemDeServicoService ordemService;

    @Mock
    private SqsBillingService sqsBillingService;

    @Mock
    private AgendaExecucaoService agendaExecucaoService;

    @Mock
    private OrdemDeServico ordem;

    @InjectMocks
    private OrdemDeServicoSagaOrchestrator orchestrator;



    // =========================================================
    // PROCESSAR RESPOSTA EXECUÇÃO
    // =========================================================

    @Test
    void deveAvancarStatusParaEmExecucaoQuandoExecucaoForAgendada() {
        var evento = criarEvento("ExecucaoAgendada", null);

        orchestrator.processarRespostaExecucao(evento);

        ArgumentCaptor<AvancarStatusDTO> captor =
                ArgumentCaptor.forClass(AvancarStatusDTO.class);

        verify(ordemService).avancarStatus(eq(OS_ID), captor.capture());

        assertThat(captor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(AvancarStatusDTO.builder()
                        .novoStatus(StatusOS.EM_EXECUCAO)
                        .build());

        verifyNoInteractions(sqsBillingService, agendaExecucaoService);
    }

    @Test
    void deveAvancarStatusParaConcluidaQuandoExecucaoForConcluida() {
        var evento = criarEvento("ExecucaoConcluida", null);

        orchestrator.processarRespostaExecucao(evento);

        ArgumentCaptor<AvancarStatusDTO> captor =
                ArgumentCaptor.forClass(AvancarStatusDTO.class);

        verify(ordemService).avancarStatus(eq(OS_ID), captor.capture());

        assertThat(captor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(AvancarStatusDTO.builder()
                        .novoStatus(StatusOS.CONCLUIDA)
                        .statusSaga(StatusSaga.AGUARDANDO_ENTREGA)
                        .build());

        verifyNoInteractions(sqsBillingService, agendaExecucaoService);
    }

    @Test
    void deveAcionarCompensacaoQuandoExecucaoForRejeitada() {
        when(ordem.getId()).thenReturn(OS_ID);
        when(ordem.getStatus()).thenReturn(StatusOS.EM_EXECUCAO);
        when(ordemService.buscarEntidade(OS_ID)).thenReturn(ordem);

        var evento = criarEvento(
                "ExecucaoRejeitada",
                criarPayload("ESTOQUE_INSUFICIENTE")
        );

        orchestrator.processarRespostaExecucao(evento);

        verify(ordemService).buscarEntidade(OS_ID);
        verify(sqsBillingService).enviarComandoProcessarPagamento(any());

        ArgumentCaptor<AvancarStatusDTO> captor =
                ArgumentCaptor.forClass(AvancarStatusDTO.class);

        verify(ordemService).avancarStatus(eq(OS_ID), captor.capture());

        assertThat(captor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(AvancarStatusDTO.builder()
                        .novoStatus(StatusOS.CANCELADA)
                        .statusSaga(StatusSaga.COMPENSADA)
                        .build());
    }

    @Test
    void deveAcionarCompensacaoQuandoExecucaoForCancelada() {
        when(ordem.getId()).thenReturn(OS_ID);
        when(ordem.getStatus()).thenReturn(StatusOS.APROVADA);
        when(ordemService.buscarEntidade(OS_ID)).thenReturn(ordem);

        var evento = criarEvento("ExecucaoCancelada", null);

        orchestrator.processarRespostaExecucao(evento);

        verify(ordemService).buscarEntidade(OS_ID);
        verify(sqsBillingService).enviarComandoProcessarPagamento(any());
        verify(ordemService).avancarStatus(eq(OS_ID), any(AvancarStatusDTO.class));
    }

    @Test
    void deveAtualizarStatusSagaParaFalhaCompensacaoQuandoCancelamentoForRecusado() {
        var evento = criarEvento(
                "CancelamentoExecucaoRecusado",
                criarPayload("CANCELAMENTO_NAO_PERMITIDO")
        );

        orchestrator.processarRespostaExecucao(evento);

        verify(ordemService).atualizarStatusSaga(
                OS_ID,
                StatusSaga.FALHA_COMPENSACAO
        );

        verify(ordemService, never()).buscarEntidade(any());
        verifyNoInteractions(sqsBillingService, agendaExecucaoService);
    }

    @Test
    void naoDeveExecutarNenhumaAcaoQuandoEventoDeExecucaoForDesconhecido() {
        var evento = criarEvento("EventoDesconhecido", null);

        orchestrator.processarRespostaExecucao(evento);

        verifyNoInteractions(ordemService, sqsBillingService, agendaExecucaoService);
    }

    @Test
    void deveLancarRuntimeExceptionQuandoIdDaOsForInvalidoNoEventoDeExecucao() {
        var evento = criarEventoComId("ExecucaoAgendada", "ID_INVALIDO", null);

        assertThatThrownBy(() -> orchestrator.processarRespostaExecucao(evento))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(NumberFormatException.class);

        verifyNoInteractions(ordemService, sqsBillingService, agendaExecucaoService);
    }

    // =========================================================
    // PROCESSAR RESPOSTA BILLING
    // =========================================================

    @Test
    void deveAvancarStatusParaAguardandoAprovacaoQuandoOrcamentoForGerado() {
        var evento = new BillingResponse.OrcamentoResponseDto(
                "orcamento-1",
                "message-1",
                "saga-1",
                "causation-1",
                "comando-1",
                OS_ID,
                CLIENTE_ID,
                StatusBilling.AGUARDANDO_APROVACAO,
                "BRL",
                new BigDecimal("150.00"),
                "2026-11-15T10:00:00",
                List.of()
        );

        orchestrator.processarRespostaBilling(evento);

        ArgumentCaptor<AvancarStatusDTO> captor =
                ArgumentCaptor.forClass(AvancarStatusDTO.class);

        verify(ordemService).avancarStatus(eq(OS_ID), captor.capture());

        assertThat(captor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(AvancarStatusDTO.builder()
                        .novoStatus(StatusOS.AGUARDANDO_APROVACAO)
                        .total(new BigDecimal("150.00"))
                        .orcamentoId("orcamento-1")
                        .build());

        verifyNoInteractions(sqsBillingService, agendaExecucaoService);
    }

    @Test
    void deveAcionarAgendaExecucaoQuandoPagamentoForAprovado() {
        var evento = criarPagamento(StatusBilling.APROVADO);

        orchestrator.processarRespostaBilling(evento);

        verify(agendaExecucaoService).executar(evento);
        verifyNoInteractions(ordemService, sqsBillingService);
    }

    @Test
    void deveAcionarCompensacaoQuandoPagamentoFalhar() {
        when(ordem.getId()).thenReturn(OS_ID);
        when(ordem.getStatus()).thenReturn(StatusOS.APROVADA);
        when(ordemService.buscarEntidade(OS_ID)).thenReturn(ordem);

        var evento = criarPagamento(StatusBilling.FALHA);

        orchestrator.processarRespostaBilling(evento);

        verify(ordemService).buscarEntidade(OS_ID);
        verify(sqsBillingService).enviarComandoProcessarPagamento(any());
        verify(ordemService).avancarStatus(eq(OS_ID), any(AvancarStatusDTO.class));

        verifyNoInteractions(agendaExecucaoService);
    }

    @Test
    void deveLancarRuntimeExceptionQuandoAgendaFalharAoProcessarPagamento() {
        var evento = criarPagamento(StatusBilling.APROVADO);

        doThrow(new IllegalStateException("Falha ao agendar execução"))
                .when(agendaExecucaoService).executar(evento);

        assertThatThrownBy(() -> orchestrator.processarRespostaBilling(evento))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    // =========================================================
    // ACIONAR COMPENSAÇÃO
    // =========================================================

    @Test
    void deveCompensarOsAprovada() {
        when(ordem.getId()).thenReturn(OS_ID);
        when(ordem.getStatus()).thenReturn(StatusOS.APROVADA);
        when(ordemService.buscarEntidade(OS_ID)).thenReturn(ordem);

        var compensacao = new CompensacaoDto(
                OS_ID.toString(),
                "EstornarPagamento"
        );

        orchestrator.acionarCompensacao(compensacao);

        verify(ordemService).buscarEntidade(OS_ID);
        verify(sqsBillingService).enviarComandoProcessarPagamento(any());

        ArgumentCaptor<AvancarStatusDTO> captor =
                ArgumentCaptor.forClass(AvancarStatusDTO.class);

        verify(ordemService).avancarStatus(eq(OS_ID), captor.capture());

        assertThat(captor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(AvancarStatusDTO.builder()
                        .novoStatus(StatusOS.CANCELADA)
                        .statusSaga(StatusSaga.COMPENSADA)
                        .build());
    }

    @Test
    void deveCompensarOsEmExecucao() {
        when(ordem.getId()).thenReturn(OS_ID);
        when(ordem.getStatus()).thenReturn(StatusOS.EM_EXECUCAO);
        when(ordemService.buscarEntidade(OS_ID)).thenReturn(ordem);

        var compensacao = new CompensacaoDto(
                OS_ID.toString(),
                "EstornarPagamento"
        );

        orchestrator.acionarCompensacao(compensacao);

        verify(sqsBillingService).enviarComandoProcessarPagamento(any());
        verify(ordemService).avancarStatus(eq(OS_ID), any(AvancarStatusDTO.class));
    }

    @Test
    void deveLancarBusinessExceptionQuandoOsNaoEstiverEmStatusPermitidoParaCompensacao() {
        when(ordem.getId()).thenReturn(OS_ID);
        when(ordem.getStatus()).thenReturn(StatusOS.ABERTA);
        when(ordemService.buscarEntidade(OS_ID)).thenReturn(ordem);

        var compensacao = new CompensacaoDto(
                OS_ID.toString(),
                "EstornarPagamento"
        );

        assertThatThrownBy(() -> orchestrator.acionarCompensacao(compensacao))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Não é possível cancelar a OS");

        verify(ordemService).buscarEntidade(OS_ID);
        verifyNoInteractions(sqsBillingService);
        verify(ordemService, never()).avancarStatus(any(), any());
    }

    @Test
    void deveFalharQuandoIdDaOsForInvalidoNaCompensacao() {
        var compensacao = new CompensacaoDto(
                "ID_INVALIDO",
                "EstornarPagamento"
        );

        assertThatThrownBy(() -> orchestrator.acionarCompensacao(compensacao))
                .isInstanceOf(NumberFormatException.class);

        verifyNoInteractions(sqsBillingService);
    }

    // =========================================================
    // MÉTODOS AUXILIARES
    // =========================================================

    private ExecucaoResultadoEventDto criarEvento(
            String tipo,
            ExecucaoResultadoEventDto.PayloadResponseDto payload
    ) {
        return criarEventoComId(tipo, OS_ID.toString(), payload);
    }

    private ExecucaoResultadoEventDto criarEventoComId(
            String tipo,
            String osId,
            ExecucaoResultadoEventDto.PayloadResponseDto payload
    ) {
        var envelope = new EnvelopeResponseSQS(
                1,
                "message-1",
                "operation-1",
                tipo,
                "saga-1",
                osId,
                "causation-1",
                Instant.now()
        );

        return new ExecucaoResultadoEventDto(envelope, payload);
    }

    private ExecucaoResultadoEventDto.PayloadResponseDto criarPayload(String motivo) {
        return new ExecucaoResultadoEventDto.PayloadResponseDto(
                "REJEITADA",
                null,
                null,
                null,
                1,
                null,
                motivo,
                null
        );
    }

    private BillingResponse.PagamentoResponseDto criarPagamento(StatusBilling status) {
        return new BillingResponse.PagamentoResponseDto(
                "pagamento-1",
                "message-1",
                "saga-1",
                "causation-1",
                "comando-1",
                OS_ID,
                CLIENTE_ID,
                "orcamento-1",
                "pagamento-1",
                status,
                "BRL",
                new BigDecimal("150.00"),
                1
        );
    }
}
