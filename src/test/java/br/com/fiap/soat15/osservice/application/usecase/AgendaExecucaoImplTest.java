
package br.com.fiap.soat15.osservice.application.usecase;

import br.com.fiap.soat15.osservice.adapter.out.persistence.repository.ItemOSRepository;
import br.com.fiap.soat15.osservice.adapter.out.persistence.repository.OrdemDeServicoRepository;
import br.com.fiap.soat15.osservice.adapter.out.persistence.repository.SagaHistoricoRepository;
import br.com.fiap.soat15.osservice.adapter.out.saga.dto.AgendarExecucaoCommandDto;
import br.com.fiap.soat15.osservice.adapter.out.saga.dto.BillingResponse;
import br.com.fiap.soat15.osservice.adapter.out.saga.out.SqsExecucaoService;
import br.com.fiap.soat15.osservice.application.dto.AvancarStatusDTO;
import br.com.fiap.soat15.osservice.domain.entity.*;
import br.com.fiap.soat15.osservice.domain.exception.BusinessException;
import br.com.fiap.soat15.osservice.domain.service.OrdemDeServicoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgendaExecucaoImplTest {

    private static final Long OS_ID = 1L;
    private static final String PAGAMENTO_ID = "pagamento-123";
    private static final String ORCAMENTO_ID = "orcamento-123";
    private static final String SAGA_ID = "saga-123";
    private static final String ITEM_ID = "item-123";
    private static final String SERVICO_ID = "servico-123";

    @Mock
    private OrdemDeServicoRepository osRepository;

    @Mock
    private SqsExecucaoService sqsProducer;

    @Mock
    private ItemOSRepository itemRepository;

    @Mock
    private SagaHistoricoRepository sagaHistoricoRepository;

    @Mock
    private OrdemDeServicoService osService;

    @InjectMocks
    private AgendaExecucaoImpl service;

    private BillingResponse.PagamentoResponseDto pagamentoResponse;

    @BeforeEach
    void setUp() {
        pagamentoResponse = mock(BillingResponse.PagamentoResponseDto.class);
    }

    @Test
    void deveLancarExcecaoQuandoOrdemNaoEstiverAprovada() {
        doReturn(OS_ID).when(pagamentoResponse).ordemId();

        when(osRepository.existsByIdAndStatus(OS_ID, StatusOS.APROVADA))
                .thenReturn(false);

        assertThatThrownBy(() -> service.executar(pagamentoResponse))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Não é possível solicitar execução para uma OS sem pagamento OS: " + OS_ID);

        verify(osRepository).existsByIdAndStatus(OS_ID, StatusOS.APROVADA);
        verifyNoInteractions(osService, sqsProducer, itemRepository, sagaHistoricoRepository);
    }

    @Test
    void deveAgendarExecucaoComItensERegistrarHistorico() {
        doReturn(OS_ID).when(pagamentoResponse).ordemId();
        doReturn(PAGAMENTO_ID).when(pagamentoResponse).pagamentoId();
        doReturn(ORCAMENTO_ID).when(pagamentoResponse).orcamentoId();
        doReturn(1).when(pagamentoResponse).versao();
        when(osRepository.existsByIdAndStatus(OS_ID, StatusOS.APROVADA))
                .thenReturn(true);

        OrdemDeServico ordem = criarOrdemComItens();

        when(osService.avancarStatus(eq(OS_ID), any(AvancarStatusDTO.class)))
                .thenReturn(ordem);

        when(itemRepository.findByOrdemDeServico(ordem))
                .thenReturn(List.of(criarItem()));

        service.executar(pagamentoResponse);

        ArgumentCaptor<AvancarStatusDTO> statusCaptor =
                ArgumentCaptor.forClass(AvancarStatusDTO.class);

        verify(osService).avancarStatus(eq(OS_ID), statusCaptor.capture());

        assertThat(statusCaptor.getValue().getNovoStatus())
                .isEqualTo(StatusOS.EM_EXECUCAO);

        assertThat(statusCaptor.getValue().getStatusSaga())
                .isEqualTo(StatusSaga.EXECUCAO_AGENDADA);

        assertThat(statusCaptor.getValue().getPagamentoId())
                .isEqualTo(PAGAMENTO_ID);

        ArgumentCaptor<AgendarExecucaoCommandDto> comandoCaptor =
                ArgumentCaptor.forClass(AgendarExecucaoCommandDto.class);

        verify(sqsProducer).enviarComandoAgendamento(comandoCaptor.capture());

        ArgumentCaptor<SagaHistorico> historicoCaptor =
                ArgumentCaptor.forClass(SagaHistorico.class);

        verify(sagaHistoricoRepository).save(historicoCaptor.capture());

        SagaHistorico historico = historicoCaptor.getValue();

        assertThat(historico.getSagaId()).isEqualTo(SAGA_ID);
        assertThat(historico.getOsId()).isEqualTo(OS_ID.toString());
        assertThat(historico.getType()).isEqualTo("AgendarExecucao");
        assertThat(historico.getOperationId())
                .isEqualTo("agendar-os-" + OS_ID + "-pagamento-" + PAGAMENTO_ID);
        assertThat(historico.getMessageId()).isNotBlank();
        assertThat(historico.getOccurredAt()).isNotNull();

        verify(itemRepository).findByOrdemDeServico(ordem);
    }

    @Test
    void deveAgendarExecucaoSemItens() {
        doReturn(OS_ID).when(pagamentoResponse).ordemId();
        doReturn(PAGAMENTO_ID).when(pagamentoResponse).pagamentoId();
        doReturn(ORCAMENTO_ID).when(pagamentoResponse).orcamentoId();
        doReturn(1).when(pagamentoResponse).versao();
        when(osRepository.existsByIdAndStatus(OS_ID, StatusOS.APROVADA))
                .thenReturn(true);

        OrdemDeServico ordem = criarOrdemSemItens();

        when(osService.avancarStatus(eq(OS_ID), any(AvancarStatusDTO.class)))
                .thenReturn(ordem);

        when(itemRepository.findByOrdemDeServico(ordem))
                .thenReturn(List.of());

        service.executar(pagamentoResponse);

        verify(sqsProducer).enviarComandoAgendamento(any(AgendarExecucaoCommandDto.class));
        verify(sagaHistoricoRepository).save(any(SagaHistorico.class));
        verify(itemRepository).findByOrdemDeServico(ordem);
    }

    @Test
    void devePropagarExcecaoQuandoFalharEnvioAoSqs() {
        doReturn(OS_ID).when(pagamentoResponse).ordemId();
        doReturn(PAGAMENTO_ID).when(pagamentoResponse).pagamentoId();
        doReturn(ORCAMENTO_ID).when(pagamentoResponse).orcamentoId();
        doReturn(1).when(pagamentoResponse).versao();
        when(osRepository.existsByIdAndStatus(OS_ID, StatusOS.APROVADA))
                .thenReturn(true);

        OrdemDeServico ordem = criarOrdemSemItens();

        when(osService.avancarStatus(eq(OS_ID), any(AvancarStatusDTO.class)))
                .thenReturn(ordem);

        when(itemRepository.findByOrdemDeServico(ordem))
                .thenReturn(List.of());

        doThrow(new RuntimeException("Falha no SQS"))
                .when(sqsProducer)
                .enviarComandoAgendamento(any(AgendarExecucaoCommandDto.class));

        assertThatThrownBy(() -> service.executar(pagamentoResponse))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Falha no SQS");

        verify(sqsProducer).enviarComandoAgendamento(any(AgendarExecucaoCommandDto.class));
        verify(sagaHistoricoRepository, never()).save(any(SagaHistorico.class));
    }

    private OrdemDeServico criarOrdemComItens() {
        Servico servico = Servico.builder()
                .id(10L)
                .idOrigem(SERVICO_ID)
                .nome("Troca de óleo")
                .build();

        ItemOS itemComServico = ItemOS.builder()
                .id(20L)
                .servico(servico)
                .tipoItem(TipoItem.SERVICO)
                .idOrigemItem(SERVICO_ID)
                .quantidade(1)
                .build();

        OrdemDeServico ordem = new OrdemDeServico();
        ordem.setId(OS_ID);
        ordem.setSagaId(SAGA_ID);
        ordem.setItens(List.of(itemComServico));

        return ordem;
    }

    private OrdemDeServico criarOrdemSemItens() {
        OrdemDeServico ordem = new OrdemDeServico();
        ordem.setId(OS_ID);
        ordem.setSagaId(SAGA_ID);
        ordem.setItens(List.of());

        return ordem;
    }

    private ItemOS criarItem() {
        return ItemOS.builder()
                .id(30L)
                .tipoItem(TipoItem.PECA)
                .idOrigemItem(ITEM_ID)
                .quantidade(2)
                .build();
    }
}
