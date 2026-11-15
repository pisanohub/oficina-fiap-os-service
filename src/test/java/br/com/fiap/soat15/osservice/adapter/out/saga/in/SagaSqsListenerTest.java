
package br.com.fiap.soat15.osservice.adapter.out.saga.in;

import br.com.fiap.soat15.osservice.adapter.out.persistence.repository.SagaHistoricoRepository;
import br.com.fiap.soat15.osservice.adapter.out.saga.dto.BillingResponse;
import br.com.fiap.soat15.osservice.adapter.out.saga.dto.EnvelopeResponseSQS;
import br.com.fiap.soat15.osservice.adapter.out.saga.dto.ExecucaoResultadoEventDto;
import br.com.fiap.soat15.osservice.domain.entity.SagaHistorico;
import br.com.fiap.soat15.osservice.domain.entity.StatusBilling;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SagaSqsListenerTest {

    private static final String JSON = "{\"messageId\":\"message-1\"}";

    private static final String MESSAGE_ID = "message-1";
    private static final String OPERATION_ID = "operation-1";
    private static final String SAGA_ID = "saga-1";
    private static final String CAUSATION_ID = "causation-1";
    private static final String OS_ID = "10";

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private OrdemDeServicoSagaOrchestrator orchestrator;

    @Mock
    private SagaHistoricoRepository sagaRepository;

    @InjectMocks
    private SagaSqsListener listener;

    private ExecucaoResultadoEventDto eventoExecucao;

    private BillingResponse.OrcamentoResponseDto eventoOrcamento;

    private BillingResponse.PagamentoResponseDto eventoPagamento;

    @BeforeEach
    void setUp() throws Exception {
        eventoExecucao = criarEventoExecucao();
        eventoOrcamento = criarEventoOrcamento();
        eventoPagamento = criarEventoPagamento();
    }

    // =========================================================
    // RESULTADOS DA EXECUÇÃO
    // =========================================================

    @Test
    void deveProcessarResultadoExecucaoESalvarHistorico() throws JsonProcessingException {
        when(sagaRepository.existsByMessageId(MESSAGE_ID)).thenReturn(false);
        when(objectMapper.readValue(JSON, ExecucaoResultadoEventDto.class))
                .thenReturn(eventoExecucao);

        listener.escutarResultadosExecucao(JSON);

        verify(objectMapper).readValue(JSON, ExecucaoResultadoEventDto.class);
        verify(sagaRepository).existsByMessageId(MESSAGE_ID);
        verify(orchestrator).processarRespostaExecucao(eventoExecucao);

        ArgumentCaptor<SagaHistorico> captor =
                ArgumentCaptor.forClass(SagaHistorico.class);

        verify(sagaRepository).save(captor.capture());

        SagaHistorico historico = captor.getValue();

        assertThat(historico.getMessageId()).isEqualTo(MESSAGE_ID);
        assertThat(historico.getOperationId()).isEqualTo(OPERATION_ID);
        assertThat(historico.getType()).isEqualTo("ExecucaoConcluida");
        assertThat(historico.getSagaId()).isEqualTo(SAGA_ID);
        assertThat(historico.getOsId()).isEqualTo(OS_ID);
        assertThat(historico.getCausationId()).isEqualTo(CAUSATION_ID);
        assertThat(historico.getOccurredAt()).isNotNull();
    }

    @Test
    void naoDeveProcessarNemSalvarHistoricoQuandoResultadoExecucaoForDuplicado() throws JsonProcessingException {
        when(sagaRepository.existsByMessageId(MESSAGE_ID)).thenReturn(true);
        when(objectMapper.readValue(JSON, ExecucaoResultadoEventDto.class))
                .thenReturn(eventoExecucao);

        listener.escutarResultadosExecucao(JSON);

        verify(objectMapper).readValue(JSON, ExecucaoResultadoEventDto.class);
        verify(sagaRepository).existsByMessageId(MESSAGE_ID);

        verifyNoInteractions(orchestrator);
        verify(sagaRepository, never()).save(any(SagaHistorico.class));
    }

    @Test
    void deveLancarRuntimeExceptionQuandoFalharAoProcessarResultadoExecucao() throws JsonProcessingException {
        when(sagaRepository.existsByMessageId(MESSAGE_ID)).thenReturn(false);
        when(objectMapper.readValue(JSON, ExecucaoResultadoEventDto.class))
                .thenReturn(eventoExecucao);

        doThrow(new IllegalStateException("Falha no processamento"))
                .when(orchestrator).processarRespostaExecucao(eventoExecucao);

        assertThatThrownBy(() -> listener.escutarResultadosExecucao(JSON))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Erro ao processar evento de resultado da execução")
                .hasCauseInstanceOf(IllegalStateException.class);

        verify(orchestrator).processarRespostaExecucao(eventoExecucao);
        verify(sagaRepository, never()).save(any(SagaHistorico.class));
    }

    // =========================================================
    // RESULTADOS DO BILLING - ORÇAMENTO
    // =========================================================

    @Test
    void deveProcessarResultadoOrcamentoESalvarHistorico() throws JsonProcessingException {
        when(sagaRepository.existsByMessageId(MESSAGE_ID)).thenReturn(false);
        when(objectMapper.readValue(JSON, BillingResponse.OrcamentoResponseDto.class))
                .thenReturn(eventoOrcamento);

        listener.escutarResultadosBillingOrcamento(JSON);

        verify(objectMapper).readValue(JSON, BillingResponse.OrcamentoResponseDto.class);
        verify(sagaRepository).existsByMessageId(MESSAGE_ID);
        verify(orchestrator).processarRespostaBilling(eventoOrcamento);

        ArgumentCaptor<SagaHistorico> captor =
                ArgumentCaptor.forClass(SagaHistorico.class);

        verify(sagaRepository).save(captor.capture());

        SagaHistorico historico = captor.getValue();

        assertThat(historico.getMessageId()).isEqualTo(MESSAGE_ID);
        assertThat(historico.getOperationId()).isEqualTo(OPERATION_ID);
        assertThat(historico.getSagaId()).isEqualTo(SAGA_ID);
        assertThat(historico.getOsId()).isEqualTo(OS_ID);
        assertThat(historico.getCausationId()).isEqualTo(CAUSATION_ID);
        assertThat(historico.getType()).isEqualTo("orcamentoGerado");
        assertThat(historico.getOccurredAt()).isNotNull();
    }

    @Test
    void naoDeveProcessarNemSalvarHistoricoQuandoOrcamentoForDuplicado() throws JsonProcessingException {
        when(sagaRepository.existsByMessageId(MESSAGE_ID)).thenReturn(true);
        when(objectMapper.readValue(JSON, BillingResponse.OrcamentoResponseDto.class))
                .thenReturn(eventoOrcamento);

        listener.escutarResultadosBillingOrcamento(JSON);

        verify(sagaRepository).existsByMessageId(MESSAGE_ID);

        verifyNoInteractions(orchestrator);
        verify(sagaRepository, never()).save(any(SagaHistorico.class));
    }

    @Test
    void deveLancarRuntimeExceptionQuandoFalharAoProcessarOrcamento() throws JsonProcessingException {
        when(sagaRepository.existsByMessageId(MESSAGE_ID)).thenReturn(false);
        when(objectMapper.readValue(JSON, BillingResponse.OrcamentoResponseDto.class))
                .thenReturn(eventoOrcamento);

        doThrow(new IllegalStateException("Falha no processamento"))
                .when(orchestrator).processarRespostaBilling(eventoOrcamento);

        assertThatThrownBy(() -> listener.escutarResultadosBillingOrcamento(JSON))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Erro ao processar evento de resultado do orcamento-respondido")
                .hasCauseInstanceOf(IllegalStateException.class);

        verify(orchestrator).processarRespostaBilling(eventoOrcamento);
        verify(sagaRepository, never()).save(any(SagaHistorico.class));
    }

    // =========================================================
    // RESULTADOS DO BILLING - PAGAMENTO
    // =========================================================

    @Test
    void deveProcessarResultadoPagamentoESalvarHistorico() throws JsonProcessingException {
        when(sagaRepository.existsByMessageId(MESSAGE_ID)).thenReturn(false);
        when(objectMapper.readValue(JSON, BillingResponse.PagamentoResponseDto.class))
                .thenReturn(eventoPagamento);

        listener.escutarResultadosBillingPagamento(JSON);

        verify(objectMapper).readValue(JSON, BillingResponse.PagamentoResponseDto.class);
        verify(sagaRepository).existsByMessageId(MESSAGE_ID);
        verify(orchestrator).processarRespostaBilling(eventoPagamento);

        ArgumentCaptor<SagaHistorico> captor =
                ArgumentCaptor.forClass(SagaHistorico.class);

        verify(sagaRepository).save(captor.capture());

        SagaHistorico historico = captor.getValue();

        assertThat(historico.getMessageId()).isEqualTo(MESSAGE_ID);
        assertThat(historico.getOperationId()).isEqualTo(OPERATION_ID);
        assertThat(historico.getSagaId()).isEqualTo(SAGA_ID);
        assertThat(historico.getOsId()).isEqualTo(OS_ID);
        assertThat(historico.getCausationId()).isEqualTo(CAUSATION_ID);
        assertThat(historico.getType()).isEqualTo("pagamentoConfirmado");
        assertThat(historico.getOccurredAt()).isNotNull();
    }

    @Test
    void naoDeveProcessarNemSalvarHistoricoQuandoPagamentoForDuplicado() throws JsonProcessingException {
        when(sagaRepository.existsByMessageId(MESSAGE_ID)).thenReturn(true);
        when(objectMapper.readValue(JSON, BillingResponse.PagamentoResponseDto.class))
                .thenReturn(eventoPagamento);

        listener.escutarResultadosBillingPagamento(JSON);

        verify(sagaRepository).existsByMessageId(MESSAGE_ID);

        verifyNoInteractions(orchestrator);
        verify(sagaRepository, never()).save(any(SagaHistorico.class));
    }

    @Test
    void deveLancarRuntimeExceptionQuandoFalharAoProcessarPagamento() throws JsonProcessingException {
        when(sagaRepository.existsByMessageId(MESSAGE_ID)).thenReturn(false);
        when(objectMapper.readValue(JSON, BillingResponse.PagamentoResponseDto.class))
                .thenReturn(eventoPagamento);

        doThrow(new IllegalStateException("Falha no processamento"))
                .when(orchestrator).processarRespostaBilling(eventoPagamento);

        assertThatThrownBy(() -> listener.escutarResultadosBillingPagamento(JSON))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Erro ao processar evento de resultado do pagamento-confirmado")
                .hasCauseInstanceOf(IllegalStateException.class);

        verify(orchestrator).processarRespostaBilling(eventoPagamento);
        verify(sagaRepository, never()).save(any(SagaHistorico.class));
    }

    // =========================================================
    // MÉTODOS AUXILIARES
    // =========================================================

    private ExecucaoResultadoEventDto criarEventoExecucao() {
        var envelope = new EnvelopeResponseSQS(
                1,
                MESSAGE_ID,
                OPERATION_ID,
                "ExecucaoConcluida",
                SAGA_ID,
                OS_ID,
                CAUSATION_ID,
                Instant.now()
        );

        var payload = new ExecucaoResultadoEventDto.PayloadResponseDto(
                "CONCLUIDA",
                "2026-11-15T10:00:00",
                "2026-11-15T11:00:00",
                "orcamento-1",
                1,
                "pagamento-1",
                null,
                "item-1"
        );

        return new ExecucaoResultadoEventDto(envelope, payload);
    }

    private BillingResponse.OrcamentoResponseDto criarEventoOrcamento() {
        return new BillingResponse.OrcamentoResponseDto(
                "orcamento-1",
                MESSAGE_ID,
                SAGA_ID,
                CAUSATION_ID,
                OPERATION_ID,
                Long.valueOf(OS_ID),
                20L,
                StatusBilling.AGUARDANDO_APROVACAO,
                "BRL",
                new BigDecimal("150.00"),
                "2026-11-15T10:00:00",
                List.of()
        );
    }

    private BillingResponse.PagamentoResponseDto criarEventoPagamento() {
        return new BillingResponse.PagamentoResponseDto(
                "pagamento-1",
                MESSAGE_ID,
                SAGA_ID,
                CAUSATION_ID,
                OPERATION_ID,
                Long.valueOf(OS_ID),
                20L,
                "orcamento-1",
                "pagamento-1",
                StatusBilling.APROVADO,
                "BRL",
                new BigDecimal("150.00"),
                1
        );
    }
}
