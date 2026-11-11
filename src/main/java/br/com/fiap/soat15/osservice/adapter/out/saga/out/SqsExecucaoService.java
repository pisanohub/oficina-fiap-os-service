package br.com.fiap.soat15.osservice.adapter.out.saga.out;

import br.com.fiap.soat15.osservice.adapter.out.saga.dto.AgendarExecucaoCommandDto;
import br.com.fiap.soat15.osservice.adapter.out.saga.dto.EnvelopeResponseSQS;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;

import java.time.Instant;
import java.util.UUID;

@Service
public class SqsExecucaoService {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    public SqsExecucaoService(SqsClient sqsClient, ObjectMapper objectMapper) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
    }

    @Value("${aws.sqs.url.out.execucao}")
    private String filaExecucaoComandosUrl;


    public void enviarComandoAgendamento(AgendarExecucaoCommandDto comando) {
        try {
            // 1. Converte o objeto Java (DTO) em uma String JSON
            String jsonPayload = objectMapper.writeValueAsString(comando);

            // 2. Cria a requisição de envio para o SQS (conforme a doc, o corpo é texto puro)
            SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                    .queueUrl(filaExecucaoComandosUrl)
                    .messageBody(jsonPayload)
                    .build();

            // 3. Envia de fato para a fila
            sqsClient.sendMessage(sendMsgRequest);

        } catch (SqsException e) {
            throw new RuntimeException("Erro da AWS ao enviar mensagem para o SQS: " + e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao serializar ou enviar comando para o SQS", e);
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
                    .queueUrl(filaExecucaoComandosUrl)
                    .messageBody(jsonPayload)
                    .build());

        } catch (Exception e) {
            throw new RuntimeException("Erro ao enviar comando de cancelamento para o SQS", e);
        }
    }
}