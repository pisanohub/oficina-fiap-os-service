
package br.com.fiap.soat15.osservice.application.usecase;

import br.com.fiap.soat15.osservice.adapter.out.gateway.dto.BillingDto;
import br.com.fiap.soat15.osservice.adapter.out.persistence.repository.ItemOSRepository;
import br.com.fiap.soat15.osservice.adapter.out.persistence.repository.OrdemDeServicoRepository;
import br.com.fiap.soat15.osservice.adapter.out.saga.dto.CancelarOsDto;
import br.com.fiap.soat15.osservice.adapter.out.saga.out.SqsBillingService;
import br.com.fiap.soat15.osservice.domain.entity.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrcamentoImplTest {

    private static final Long OS_ID = 1L;
    private static final Long CLIENTE_ID = 10L;

    private static final String SAGA_ID = "saga-123";
    private static final String SERVICO_ID = "servico-123";
    private static final String ITEM_ID = "item-123";

    @Mock
    private ItemOSRepository itemOSRepository;

    @Mock
    private OrdemDeServicoRepository osRepository;

    @Mock
    private SqsBillingService sqsProducer;

    @InjectMocks
    private OrcamentoImpl service;

    @Test
    void deveLancarExcecaoQuandoSolicitarOrcamentoSemItens() {
        OrdemDeServico ordem = criarOrdem();

        when(itemOSRepository.buscarPorOrdemComServico(ordem))
                .thenReturn(new ArrayList<>());

        assertThatThrownBy(() -> service.solicitarOrcamento(ordem, CLIENTE_ID))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("sem itens ou serviços");

        verify(itemOSRepository).buscarPorOrdemComServico(ordem);
        verifyNoInteractions(osRepository, sqsProducer);
    }

    @Test
    void deveSolicitarOrcamentoComSagaIdExistente() {
        OrdemDeServico ordem = criarOrdem();
        ordem.setSagaId(SAGA_ID);

        Servico servico = criarServico();

        ItemOS item = criarItemEstoque();

        ItemOS itemComServico = criarItemServico(servico);

        when(itemOSRepository.buscarPorOrdemComServico(ordem))
                .thenReturn(new ArrayList<>(List.of(item)));

        ordem.setItens(List.of(itemComServico));

        service.solicitarOrcamento(ordem, CLIENTE_ID);

        assertThat(ordem.getSagaId()).isEqualTo(SAGA_ID);
        assertThat(ordem.getStatusSaga())
                .isEqualTo(StatusSaga.ENVIADO_PARA_ORCAMENTO);

        verify(osRepository).save(ordem);

        ArgumentCaptor<BillingDto.GerarOrcamentoRequest> captor =
                ArgumentCaptor.forClass(BillingDto.GerarOrcamentoRequest.class);

        verify(sqsProducer).enviarComandoGerarOrcamento(captor.capture());

        assertThat(captor.getValue()).isNotNull();

        verify(itemOSRepository).buscarPorOrdemComServico(ordem);
    }

    @Test
    void deveGerarSagaIdQuandoOrdemNaoPossuirSagaId() {
        OrdemDeServico ordem = criarOrdem();
        ordem.setSagaId(null);

        when(itemOSRepository.buscarPorOrdemComServico(ordem))
                .thenReturn(new ArrayList<>(List.of(criarItemEstoque())));

        ordem.setItens(List.of());

        service.solicitarOrcamento(ordem, CLIENTE_ID);

        assertThat(ordem.getSagaId()).isEqualTo("saga-" + OS_ID);

        assertThat(ordem.getStatusSaga())
                .isEqualTo(StatusSaga.ENVIADO_PARA_ORCAMENTO);

        verify(osRepository).save(ordem);
        verify(sqsProducer).enviarComandoGerarOrcamento(any(BillingDto.GerarOrcamentoRequest.class));
    }

    @Test
    void deveAdicionarServicosAoOrcamentoSemDuplicarServicos() {
        OrdemDeServico ordem = criarOrdem();

        Servico servico = criarServico();

        ItemOS itemComServico1 = criarItemServico(servico);
        ItemOS itemComServico2 = criarItemServico(servico);

        ordem.setItens(List.of(itemComServico1, itemComServico2));

        ItemOS itemEstoque = criarItemEstoque();

        when(itemOSRepository.buscarPorOrdemComServico(ordem))
                .thenReturn(new ArrayList<>(List.of(itemEstoque)));

        service.solicitarOrcamento(ordem, CLIENTE_ID);

        ArgumentCaptor<BillingDto.GerarOrcamentoRequest> captor =
                ArgumentCaptor.forClass(BillingDto.GerarOrcamentoRequest.class);

        verify(sqsProducer).enviarComandoGerarOrcamento(captor.capture());

        assertThat(captor.getValue()).isNotNull();

        verify(osRepository).save(ordem);
    }

    @Test
    void deveProcessarPagamentoEAtualizarStatusSaga() {
        OrdemDeServico ordem = criarOrdem();

        service.processarPagamento(ordem, CLIENTE_ID);

        assertThat(ordem.getStatusSaga())
                .isEqualTo(StatusSaga.ENVIADO_PARA_PAGAMENTO);

        ArgumentCaptor<CancelarOsDto> captor =
                ArgumentCaptor.forClass(CancelarOsDto.class);

        verify(sqsProducer).enviarComandoProcessarPagamento(captor.capture());

        assertThat(captor.getValue()).isNotNull();

        verify(osRepository).save(ordem);
    }

    private OrdemDeServico criarOrdem() {
        OrdemDeServico ordem = new OrdemDeServico();
        ordem.setId(OS_ID);
        ordem.setSagaId(SAGA_ID);
        ordem.setStatus(StatusOS.APROVADA);
        ordem.setItens(new ArrayList<>());

        return ordem;
    }

    private Servico criarServico() {
        return Servico.builder()
                .id(20L)
                .idOrigem(SERVICO_ID)
                .nome("Troca de óleo")
                .descricao("Troca de óleo do motor")
                .preco(new BigDecimal("150.00"))
                .tempoEstimadoMinutos(30)
                .build();
    }

    private ItemOS criarItemEstoque() {
        return ItemOS.builder()
                .id(30L)
                .tipoItem(TipoItem.PECA)
                .idOrigemItem(ITEM_ID)
                .nomeItem("Filtro de óleo")
                .quantidade(2)
                .precoUnitario(new BigDecimal("50.00"))
                .subtotal(new BigDecimal("100.00"))
                .build();
    }

    private ItemOS criarItemServico(Servico servico) {
        return ItemOS.builder()
                .id(40L)
                .servico(servico)
                .tipoItem(TipoItem.SERVICO)
                .idOrigemItem(SERVICO_ID)
                .nomeItem(servico.getNome())
                .quantidade(1)
                .precoUnitario(servico.getPreco())
                .subtotal(servico.getPreco())
                .build();
    }
}
