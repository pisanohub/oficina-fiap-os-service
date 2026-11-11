package br.com.fiap.soat15.osservice.adapter.out.saga.in;

import br.com.fiap.soat15.osservice.adapter.out.persistence.repository.SagaHistoricoRepository;
import br.com.fiap.soat15.osservice.adapter.out.saga.dto.BillingResponse;
import br.com.fiap.soat15.osservice.adapter.out.saga.dto.EnvelopeResponseSQS;
import br.com.fiap.soat15.osservice.adapter.out.saga.dto.ExecucaoResultadoEventDto;
import br.com.fiap.soat15.osservice.domain.entity.SagaHistorico;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class SagaSqsListener {

    private final ObjectMapper objectMapper;
    private final OrdemDeServicoSagaOrchestrator orchestrator;
    private final SagaHistoricoRepository sagaRepository;

    public SagaSqsListener(ObjectMapper objectMapper, OrdemDeServicoSagaOrchestrator orchestrator, SagaHistoricoRepository sagaRepository) {
        this.objectMapper = objectMapper;
        this.orchestrator = orchestrator;
        this.sagaRepository = sagaRepository;
    }


    @SqsListener("${aws.sqs.url.in.execucao}")
    public void escutarResultadosExecucao(String mensagemJson) {
        try {
            ExecucaoResultadoEventDto evento = objectMapper.readValue(mensagemJson, ExecucaoResultadoEventDto.class);

            String messageId = evento.envelopeResponseSQS().messageId();
            if (sagaRepository.existsByMessageId(messageId)) {
                System.out.println("Mensagem duplicada detectada e ignorada: " + messageId);
                return; // Apenas ignora (o SQS remove a mensagem da fila com sucesso)
            }

            orchestrator.processarRespostaExecucao(evento);

            salvarSagaHistoricoExecucao(evento);

        } catch (Exception e) {
            // Se falhar ao processar, a mensagem volta para o SQS (ou vai para a DLQ após 3 tentativas)
            throw new RuntimeException("Erro ao processar evento de resultado da execução", e);
        }
    }

    @SqsListener("${aws.sqs.url.in.billing.orcamento-respondido}")
    public void escutarResultadosBillingOrcamento(String mensagemJson) {
        try {

            System.out.println("Mensagem recebida do SQS: " + mensagemJson);

            BillingResponse.OrcamentoResponseDto evento = objectMapper.readValue(mensagemJson, BillingResponse.OrcamentoResponseDto.class);

            String messageId = evento.messageId();
            if (sagaRepository.existsByMessageId(messageId)) {
                System.out.println("Mensagem duplicada detectada e ignorada: " + messageId);
                return; // Apenas ignora (o SQS remove a mensagem da fila com sucesso)
            }

            orchestrator.processarRespostaBilling(evento);

            salvarSagaHistoricoBilling(evento, "orcamentoGerado");

        } catch (Exception e) {
            // Se falhar ao processar, a mensagem volta para o SQS (ou vai para a DLQ após 3 tentativas)
            throw new RuntimeException("Erro ao processar evento de resultado do orcamento-respondido", e);
        }
    }

    @SqsListener("${aws.sqs.url.in.billing.pagamento-confirmado}")
    public void escutarResultadosBillingPagamento(String mensagemJson) {
        try {
            BillingResponse.PagamentoResponseDto evento = objectMapper.readValue(mensagemJson, BillingResponse.PagamentoResponseDto.class);

            String messageId = evento.messageId();
            if (sagaRepository.existsByMessageId(messageId)) {
                System.out.println("Mensagem duplicada detectada e ignorada: " + messageId);
                return; // Apenas ignora (o SQS remove a mensagem da fila com sucesso)
            }

            orchestrator.processarRespostaBilling(evento);

            salvarSagaHistoricoBilling(evento, "pagamentoConfirmado");
        } catch (Exception e) {
            // Se falhar ao processar, a mensagem volta para o SQS (ou vai para a DLQ após 3 tentativas)
            throw new RuntimeException("Erro ao processar evento de resultado do pagamento-confirmado", e);
        }
    }

    private void salvarSagaHistoricoBilling(BillingResponse.MessageDto evento, String type) {
        SagaHistorico sagaHistorico = SagaHistorico.builder()
                .messageId(evento.messageId())
                .operationId(evento.comandoId())
                .sagaId(evento.sagaId())
                .osId(evento.ordemId().toString())
                .causationId(evento.causationId())
                .occurredAt(Instant.now())
                .type(type)
                .build();

        sagaRepository.save(sagaHistorico);
    }


    private void salvarSagaHistoricoExecucao(ExecucaoResultadoEventDto evento) {
        EnvelopeResponseSQS envelope = evento.envelopeResponseSQS();

        SagaHistorico sagaHistorico = SagaHistorico.builder()
                .messageId(envelope.messageId())
                .operationId(envelope.operationId())
                .type(envelope.type())
                .sagaId(envelope.sagaId())
                .osId(envelope.osId())
                .causationId(envelope.causationId())
                .occurredAt(envelope.occurredAt())
                .build();

        sagaRepository.save(sagaHistorico);
    }
}