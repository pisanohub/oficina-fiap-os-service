package br.com.fiap.soat15.osservice.adapter.out.saga.out;

import br.com.fiap.soat15.osservice.adapter.out.gateway.dto.BillingDto;
import br.com.fiap.soat15.osservice.adapter.out.saga.dto.EnvelopeResponseSQS;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;

import java.time.Instant;
import java.util.UUID;

@Service
public class SqsBillingService {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    public SqsBillingService(SqsClient sqsClient, ObjectMapper objectMapper) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
    }

    @Value("${aws.sqs.url.out.billing.gerar-orcamento}")
    private String gerarOrcamentoUrl;

    @Value("${aws.sqs.url.out.billing.processar-pagamento}")
    private String processarPagamentoUrl;

    public void enviarComandoGerarOrcamento(BillingDto.GerarOrcamentoRequest comando) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(comando);
            enviarComando(jsonPayload, gerarOrcamentoUrl);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao serializar ou enviar comando para o SQS", e);
        }
    }

    public void enviarComandoProcessarPagamento(BillingDto.ProcessarPagamentoRequest comando) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(comando);
            enviarComando(jsonPayload, processarPagamentoUrl);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao serializar ou enviar comando para o SQS", e);
        }
    }

    private void enviarComando(String jsonPayload, String url){
        try {
            SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                    .queueUrl(url)
                    .messageBody(jsonPayload)
                    .build();

            sqsClient.sendMessage(sendMsgRequest);

        } catch (SqsException e) {
            throw new RuntimeException("Erro da AWS ao enviar mensagem para o SQS: " + e.awsErrorDetails().errorMessage(), e);
        }
    }



    public void enviarComandoCancelamento(String osId, String sagaId, String causationId) {
        try {
            EnvelopeResponseSQS envelopeResponseSQS = new EnvelopeResponseSQS(
                    1,
                    UUID.randomUUID().toString(),
                    "cancelar-os-" + osId,
                    "CancelarExecucao",
                    sagaId,
                    osId,
                    causationId,
                    Instant.now()
            );

            String jsonPayload = objectMapper.writeValueAsString(envelopeResponseSQS);

            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(gerarOrcamentoUrl)
                    .messageBody(jsonPayload)
                    .build());

        } catch (Exception e) {
            throw new RuntimeException("Erro ao enviar comando de cancelamento para o SQS", e);
        }
    }
}