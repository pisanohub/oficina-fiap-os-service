
package br.com.fiap.soat15.osservice.adapter.out.saga.out;

import br.com.fiap.soat15.osservice.adapter.out.gateway.dto.BillingDto;
import br.com.fiap.soat15.osservice.adapter.out.saga.dto.CancelarOsDto;
import br.com.fiap.soat15.osservice.domain.entity.OrdemDeServico;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SqsBillingServiceTest {

    private static final String GERAR_ORCAMENTO_URL =
            "http://localhost:4566/000000000000/gerar-orcamento";

    private static final String PROCESSAR_PAGAMENTO_URL =
            "http://localhost:4566/000000000000/processar-pagamento";

    private static final String JSON = "{\"message\":\"teste\"}";

    private static final Long OS_ID = 10L;
    private static final Long CLIENTE_ID = 20L;

    @Mock
    private SqsClient sqsClient;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SqsBillingService sqsBillingService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                sqsBillingService,
                "gerarOrcamentoUrl",
                GERAR_ORCAMENTO_URL
        );

        ReflectionTestUtils.setField(
                sqsBillingService,
                "processarPagamentoUrl",
                PROCESSAR_PAGAMENTO_URL
        );
    }

    // =========================================================
    // ENVIAR COMANDO GERAR ORÇAMENTO
    // =========================================================

    @Test
    void deveEnviarComandoGerarOrcamentoParaFilaSqs() throws Exception {
        BillingDto.GerarOrcamentoRequest comando =
                mock(BillingDto.GerarOrcamentoRequest.class);

        when(objectMapper.writeValueAsString(comando))
                .thenReturn(JSON);

        sqsBillingService.enviarComandoGerarOrcamento(comando);

        verify(objectMapper).writeValueAsString(comando);

        ArgumentCaptor<SendMessageRequest> captor =
                ArgumentCaptor.forClass(SendMessageRequest.class);

        verify(sqsClient).sendMessage(captor.capture());

        SendMessageRequest request = captor.getValue();

        assertThat(request.queueUrl()).isEqualTo(GERAR_ORCAMENTO_URL);
        assertThat(request.messageBody()).isEqualTo(JSON);
    }

    @Test
    void deveLancarRuntimeExceptionQuandoFalharSerializacaoDoComandoGerarOrcamento() throws Exception {
        BillingDto.GerarOrcamentoRequest comando =
                mock(BillingDto.GerarOrcamentoRequest.class);

        JsonProcessingException exception =
                new JsonProcessingException("Erro ao serializar JSON") {};

        when(objectMapper.writeValueAsString(comando))
                .thenThrow(exception);

        assertThatThrownBy(() ->
                sqsBillingService.enviarComandoGerarOrcamento(comando))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Erro ao serializar ou enviar comando para o SQS")
                .hasCause(exception);

        verify(objectMapper).writeValueAsString(comando);
        verifyNoInteractions(sqsClient);
    }

    @Test
    void deveLancarRuntimeExceptionQuandoAwsFalharAoEnviarComandoGerarOrcamento() throws Exception {
        BillingDto.GerarOrcamentoRequest comando =
                mock(BillingDto.GerarOrcamentoRequest.class);

        when(objectMapper.writeValueAsString(comando))
                .thenReturn(JSON);

        RuntimeException exception = new RuntimeException("Erro ao serializar ou enviar comando para o SQS");

        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenThrow(exception);

        assertThatThrownBy(() ->
                sqsBillingService.enviarComandoGerarOrcamento(comando))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Erro ao serializar ou enviar comando para o SQS")
                .hasCause(exception);

        verify(sqsClient).sendMessage(any(SendMessageRequest.class));
    }

    // =========================================================
    // ENVIAR COMANDO PROCESSAR PAGAMENTO
    // =========================================================

    @Test
    void deveEnviarComandoProcessarPagamentoParaFilaSqs() throws Exception {
        CancelarOsDto dto = criarCancelarOsDto();

        when(objectMapper.writeValueAsString(
                any(BillingDto.ProcessarPagamentoRequest.class)))
                .thenReturn(JSON);

        sqsBillingService.enviarComandoProcessarPagamento(dto);

        ArgumentCaptor<BillingDto.ProcessarPagamentoRequest> comandoCaptor =
                ArgumentCaptor.forClass(BillingDto.ProcessarPagamentoRequest.class);

        verify(objectMapper).writeValueAsString(comandoCaptor.capture());

        assertThat(comandoCaptor.getValue()).isNotNull();

        ArgumentCaptor<SendMessageRequest> requestCaptor =
                ArgumentCaptor.forClass(SendMessageRequest.class);

        verify(sqsClient).sendMessage(requestCaptor.capture());

        SendMessageRequest request = requestCaptor.getValue();

        assertThat(request.queueUrl()).isEqualTo(PROCESSAR_PAGAMENTO_URL);
        assertThat(request.messageBody()).isEqualTo(JSON);
    }

    @Test
    void deveLancarRuntimeExceptionQuandoFalharSerializacaoDoComandoProcessarPagamento() throws Exception {
        CancelarOsDto dto = criarCancelarOsDto();

        JsonProcessingException exception =
                new JsonProcessingException("Erro ao serializar JSON") {};

        when(objectMapper.writeValueAsString(
                any(BillingDto.ProcessarPagamentoRequest.class)))
                .thenThrow(exception);

        assertThatThrownBy(() ->
                sqsBillingService.enviarComandoProcessarPagamento(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Erro ao serializar ou enviar comando para o SQS")
                .hasCause(exception);

        verify(objectMapper).writeValueAsString(
                any(BillingDto.ProcessarPagamentoRequest.class));

        verifyNoInteractions(sqsClient);
    }

    @Test
    void deveLancarRuntimeExceptionQuandoAwsFalharAoEnviarComandoProcessarPagamento() throws Exception {
        CancelarOsDto dto = criarCancelarOsDto();

        when(objectMapper.writeValueAsString(
                any(BillingDto.ProcessarPagamentoRequest.class)))
                .thenReturn(JSON);

        RuntimeException exception = new RuntimeException("Fila indisponível");

        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenThrow(exception);

        assertThatThrownBy(() ->
                sqsBillingService.enviarComandoProcessarPagamento(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Erro ao serializar ou enviar comando para o SQS")
                .hasCause(exception);

        verify(sqsClient).sendMessage(any(SendMessageRequest.class));
    }

    // =========================================================
    // MÉTODOS AUXILIARES
    // =========================================================

    private CancelarOsDto criarCancelarOsDto() {
        OrdemDeServico ordem = mock(
                OrdemDeServico.class,
                RETURNS_DEEP_STUBS
        );

        when(ordem.getId()).thenReturn(OS_ID);
        when(ordem.getSagaId()).thenReturn("saga-1");
        when(ordem.getVeiculo().getCliente().getId())
                .thenReturn(CLIENTE_ID);

        CancelarOsDto dto = mock(CancelarOsDto.class);

        when(dto.getOrdemDeServico()).thenReturn(ordem);
        when(dto.getType()).thenReturn("EstornarPagamento");

        return dto;
    }

    private SqsException criarSqsException(String mensagem) {
        AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                .errorMessage(mensagem)
                .serviceName("SQS")
                .build();

        return (SqsException) SqsException.builder()
                .message(mensagem)
                .awsErrorDetails(errorDetails)
                .build();
    }
}
