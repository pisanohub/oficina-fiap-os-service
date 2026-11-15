
package br.com.fiap.soat15.osservice.application.usecase;

import br.com.fiap.soat15.osservice.adapter.out.gateway.ExecucaoClient;
import br.com.fiap.soat15.osservice.adapter.out.gateway.dto.CatalogoResponseDTO;
import br.com.fiap.soat15.osservice.adapter.out.persistence.repository.*;
import br.com.fiap.soat15.osservice.adapter.out.saga.dto.CancelarOsDto;
import br.com.fiap.soat15.osservice.adapter.out.saga.out.SqsBillingService;
import br.com.fiap.soat15.osservice.adapter.out.saga.out.SqsExecucaoService;
import br.com.fiap.soat15.osservice.application.dto.*;
import br.com.fiap.soat15.osservice.domain.entity.*;
import br.com.fiap.soat15.osservice.domain.exception.BusinessException;
import feign.FeignException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdemDeServicoServiceImplTest {

    private static final Long OS_ID = 1L;
    private static final Long CLIENTE_ID = 10L;
    private static final Long VEICULO_ID = 20L;
    private static final String ITEM_ID = "ITEM-01";
    private static final String SERVICO_ID = "SERVICO-01";

    @Mock
    private OrdemDeServicoRepository ordemRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private VeiculoRepository veiculoRepository;

    @Mock
    private ItemOSRepository itemOSRepository;

    @Mock
    private ServicoRepository servicoRepository;

    @Mock
    private OrcamentoImpl billingService;

    @Mock
    private SqsExecucaoService sqsExecucaoService;

    @Mock
    private SqsBillingService sqsBillingService;

    @Mock
    private ExecucaoClient execucaoClient;

    private SimpleMeterRegistry meterRegistry;

    private OrdemDeServicoServiceImpl service;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();

        service = new OrdemDeServicoServiceImpl(
                ordemRepository,
                clienteRepository,
                veiculoRepository,
                itemOSRepository,
                servicoRepository,
                meterRegistry,
                billingService,
                sqsExecucaoService,
                sqsBillingService,
                execucaoClient
        );
    }

    // =========================================================
    // CRIAR ORDEM
    // =========================================================

    @Test
    void deveCriarOrdemSemItens() {
        Veiculo veiculo = criarVeiculo();

        CriarOrdemDTO dto = CriarOrdemDTO.builder()
                .clienteId(CLIENTE_ID)
                .veiculoId(VEICULO_ID)
                .descricaoProblema("Problema no motor")
                .observacoes("Observação")
                .build();

        when(veiculoRepository.findById(VEICULO_ID))
                .thenReturn(Optional.of(veiculo));
        when(ordemRepository.count()).thenReturn(0L);
        when(ordemRepository.save(any(OrdemDeServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrdemDeServicoDTO resultado = service.criarOrdem(dto);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getStatus()).isEqualTo(StatusOS.ABERTA);
        assertThat(resultado.getDescricaoProblema()).isEqualTo("Problema no motor");
        assertThat(resultado.getClienteId()).isEqualTo(CLIENTE_ID);
        assertThat(resultado.getVeiculoId()).isEqualTo(VEICULO_ID);

        verify(ordemRepository).save(any(OrdemDeServico.class));
        assertThat(meterRegistry.counter("oficina.ordens.criadas").count())
                .isEqualTo(1);
    }

    @Test
    void deveLancarExcecaoQuandoVeiculoNaoExistirAoCriarOrdem() {
        CriarOrdemDTO dto = CriarOrdemDTO.builder()
                .clienteId(CLIENTE_ID)
                .veiculoId(VEICULO_ID)
                .descricaoProblema("Problema")
                .build();

        when(veiculoRepository.findById(VEICULO_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criarOrdem(dto))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Veículo não encontrado");

        verify(ordemRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoQuandoVeiculoNaoPertencerAoCliente() {
        Veiculo veiculo = criarVeiculo();

        CriarOrdemDTO dto = CriarOrdemDTO.builder()
                .clienteId(999L)
                .veiculoId(VEICULO_ID)
                .descricaoProblema("Problema")
                .build();

        when(veiculoRepository.findById(VEICULO_ID))
                .thenReturn(Optional.of(veiculo));

        assertThatThrownBy(() -> service.criarOrdem(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não pertence ao cliente");

        verify(ordemRepository, never()).save(any());
    }

    @Test
    void deveCriarOrdemComItensCadastrados() {
        Veiculo veiculo = criarVeiculo();

        ItensEstoqueCadastro itemCadastro = ItensEstoqueCadastro.builder()
                .itemEstoqueId(ITEM_ID)
                .tipoItem(TipoItem.PECA)
                .quantidade(2)
                .build();

        CriarOrdemDTO dto = CriarOrdemDTO.builder()
                .clienteId(CLIENTE_ID)
                .veiculoId(VEICULO_ID)
                .servicoId(SERVICO_ID)
                .descricaoProblema("Problema")
                .itensEstoqueCadastro(List.of(itemCadastro))
                .build();

        OrdemDeServico ordem = criarOrdem(StatusOS.ABERTA);

        when(veiculoRepository.findById(VEICULO_ID))
                .thenReturn(Optional.of(veiculo));
        when(ordemRepository.count()).thenReturn(0L);
        when(ordemRepository.save(any(OrdemDeServico.class)))
                .thenAnswer(invocation -> {
                    OrdemDeServico salva = invocation.getArgument(0);
                    salva.setId(OS_ID);
                    return salva;
                });

        mockServicoEEstoque();

        when(servicoRepository.findAll(any(org.springframework.data.domain.Example.class)))
                .thenReturn(List.of(criarServico()));

        when(itemOSRepository.save(any(ItemOS.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(ordemRepository.findById(OS_ID))
                .thenReturn(Optional.of(ordem));

        OrdemDeServicoDTO resultado = service.criarOrdem(dto);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getStatus()).isEqualTo(StatusOS.EM_DIAGNOSTICO);
        verify(itemOSRepository).save(any(ItemOS.class));
    }

    // =========================================================
    // GET ADICIONAR ITEM DTO
    // =========================================================

    @Test
    void deveConverterCriarOrdemDTOParaAdicionarItemDTO() {
        ItensEstoqueCadastro item = ItensEstoqueCadastro.builder()
                .itemEstoqueId(ITEM_ID)
                .tipoItem(TipoItem.PECA)
                .quantidade(3)
                .build();

        CriarOrdemDTO dto = CriarOrdemDTO.builder()
                .servicoId(SERVICO_ID)
                .itensEstoqueCadastro(List.of(item))
                .build();

        AdicionarItemDTO resultado = service.getAdicionarItemDTO(dto);

        assertThat(resultado.getItens()).hasSize(1);
        assertThat(resultado.getItens().getFirst().getServicoId())
                .isEqualTo(SERVICO_ID);
        assertThat(resultado.getItens().getFirst().getItemEstoqueId())
                .isEqualTo(ITEM_ID);
        assertThat(resultado.getItens().getFirst().getQuantidade())
                .isEqualTo(3);
    }

    // =========================================================
    // CANCELAMENTO
    // =========================================================

    @ParameterizedTest
    @EnumSource(value = StatusOS.class, names = {
            "ABERTA",
            "EM_DIAGNOSTICO",
            "AGUARDANDO_APROVACAO"
    })
    void deveCancelarOrdemEmStatusQueNaoExigeEstorno(StatusOS status) {
        OrdemDeServico ordem = criarOrdem(status);

        when(ordemRepository.findById(OS_ID))
                .thenReturn(Optional.of(ordem));
        when(ordemRepository.save(any(OrdemDeServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.cancelarOrdem(OS_ID);

        assertThat(ordem.getStatus()).isEqualTo(StatusOS.CANCELADA);
        assertThat(ordem.getStatusSaga()).isEqualTo(StatusSaga.COMPENSADA);
    }

    @Test
    void deveCancelarOrdemAprovadaRealizandoEstorno() {
        OrdemDeServico ordem = criarOrdem(StatusOS.APROVADA);

        when(ordemRepository.findById(OS_ID))
                .thenReturn(Optional.of(ordem));
        when(ordemRepository.save(any(OrdemDeServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.cancelarOrdem(OS_ID);

        verify(sqsBillingService).enviarComandoProcessarPagamento(
                any(CancelarOsDto.class));
        assertThat(ordem.getStatus()).isEqualTo(StatusOS.CANCELADA);
    }

    @Test
    void deveSolicitarCancelamentoDeExecucao() {
        OrdemDeServico ordem = criarOrdem(StatusOS.EM_EXECUCAO);

        when(ordemRepository.findById(OS_ID))
                .thenReturn(Optional.of(ordem));
        when(ordemRepository.save(any(OrdemDeServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.cancelarOrdem(OS_ID);

        verify(sqsExecucaoService).enviarComandoCancelamento(
                eq(OS_ID.toString()),
                eq(ordem.getSagaId()),
                eq("Cancelamento-Solicitado-Pelo-Cliente"),
                eq("CancelarExecucao")
        );

        assertThat(ordem.getStatusSaga())
                .isEqualTo(StatusSaga.CANCELAMENTO_EXECUCAO_PENDENTE);
        assertThat(ordem.getStatus()).isEqualTo(StatusOS.EM_EXECUCAO);
    }

    @ParameterizedTest
    @EnumSource(value = StatusOS.class, names = {
            "CONCLUIDA",
            "ENTREGUE",
            "CANCELADA",
            "FALHA_SAGA"
    })
    void deveImpedirCancelamentoEmStatusNaoPermitido(StatusOS status) {
        OrdemDeServico ordem = criarOrdem(status);

        when(ordemRepository.findById(OS_ID))
                .thenReturn(Optional.of(ordem));

        assertThatThrownBy(() -> service.cancelarOrdem(OS_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Não foi possível processar a solicitação");

        verify(sqsBillingService, never())
                .enviarComandoProcessarPagamento(any());
        verify(sqsExecucaoService, never())
                .enviarComandoCancelamento(any(), any(), any(), any());
    }

    // =========================================================
    // ORÇAMENTO
    // =========================================================

    @Test
    void deveSolicitarOrcamento() {
        OrdemDeServico ordem = criarOrdem(StatusOS.EM_DIAGNOSTICO);

        when(ordemRepository.findById(OS_ID))
                .thenReturn(Optional.of(ordem));

        OrdemDeServicoDTO resultado = service.solicitarOrcamento(OS_ID);

        verify(billingService).solicitarOrcamento(ordem, CLIENTE_ID);
        assertThat(resultado).isNotNull();
    }

    @Test
    void deveListarOrcamentosAbertosPorCliente() {
        OrdemDeServico ordem = criarOrdem(StatusOS.AGUARDANDO_APROVACAO);

        when(clienteRepository.existsById(CLIENTE_ID)).thenReturn(true);
        when(ordemRepository.findByVeiculoClienteIdAndStatus(
                CLIENTE_ID, StatusOS.AGUARDANDO_APROVACAO))
                .thenReturn(List.of(ordem));

        List<OrdemDeServicoDTO> resultado =
                service.listarOrcamentosAbertosPorCliente(CLIENTE_ID);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.getFirst().getStatus())
                .isEqualTo(StatusOS.AGUARDANDO_APROVACAO);
    }

    @Test
    void deveLancarExcecaoAoListarOrcamentosDeClienteInexistente() {
        when(clienteRepository.existsById(CLIENTE_ID)).thenReturn(false);

        assertThatThrownBy(() ->
                service.listarOrcamentosAbertosPorCliente(CLIENTE_ID))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Cliente não encontrado");
    }

    @Test
    void deveAprovarOrcamentoEEnviarParaPagamento() {
        OrdemDeServico ordem = criarOrdem(StatusOS.AGUARDANDO_APROVACAO);

        when(ordemRepository.existsByIdAndStatus(
                OS_ID, StatusOS.AGUARDANDO_APROVACAO))
                .thenReturn(true);
        when(ordemRepository.findById(OS_ID))
                .thenReturn(Optional.of(ordem));
        when(ordemRepository.save(any(OrdemDeServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrdemDeServicoDTO resultado =
                service.aprovarOrcamento(OS_ID, StatusOrcamento.APROVADO);

        verify(billingService).processarPagamento(ordem, CLIENTE_ID);
        assertThat(resultado.getStatus()).isEqualTo(StatusOS.APROVADA);
        assertThat(ordem.getStatusSaga())
                .isEqualTo(StatusSaga.ENVIADO_PARA_PAGAMENTO);
    }

    @Test
    void deveRejeitarOrcamentoCancelandoOrdem() {
        OrdemDeServico ordem = criarOrdem(StatusOS.AGUARDANDO_APROVACAO);

        when(ordemRepository.existsByIdAndStatus(
                OS_ID, StatusOS.AGUARDANDO_APROVACAO))
                .thenReturn(true);
        when(ordemRepository.findById(OS_ID))
                .thenReturn(Optional.of(ordem));
        when(ordemRepository.save(any(OrdemDeServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrdemDeServicoDTO resultado =
                service.aprovarOrcamento(OS_ID, StatusOrcamento.REJEITADO);

        assertThat(resultado.getStatus()).isEqualTo(StatusOS.CANCELADA);
        assertThat(ordem.getStatusSaga()).isEqualTo(StatusSaga.COMPENSADA);
        verify(billingService, never()).processarPagamento(any(), any());
    }

    @Test
    void deveImpedirAprovacaoQuandoOrdemNaoEstiverAguardandoAprovacao() {
        when(ordemRepository.existsByIdAndStatus(
                OS_ID, StatusOS.AGUARDANDO_APROVACAO))
                .thenReturn(false);

        assertThatThrownBy(() ->
                service.aprovarOrcamento(OS_ID, StatusOrcamento.APROVADO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Não é possivel aprovar");
    }

    // =========================================================
    // CONSULTAS
    // =========================================================

    @Test
    void deveListarOrdensConcluidasPorCliente() {
        OrdemDeServico ordem = criarOrdem(StatusOS.CONCLUIDA);

        when(clienteRepository.existsById(CLIENTE_ID)).thenReturn(true);
        when(ordemRepository.findByVeiculoClienteIdAndStatus(
                CLIENTE_ID, StatusOS.CONCLUIDA))
                .thenReturn(List.of(ordem));

        List<OrdemDeServicoDTO> resultado =
                service.listarOsConcluidasPorCliente(CLIENTE_ID);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.getFirst().getStatus()).isEqualTo(StatusOS.CONCLUIDA);
    }

    @Test
    void deveLancarExcecaoAoListarOrdensConcluidasDeClienteInexistente() {
        when(clienteRepository.existsById(CLIENTE_ID)).thenReturn(false);

        assertThatThrownBy(() ->
                service.listarOsConcluidasPorCliente(CLIENTE_ID))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void deveFinalizarOrdemConcluidaComoEntregue() {
        OrdemDeServico ordem = criarOrdem(StatusOS.CONCLUIDA);

        when(ordemRepository.existsByIdAndStatus(OS_ID, StatusOS.CONCLUIDA))
                .thenReturn(true);
        when(ordemRepository.findById(OS_ID))
                .thenReturn(Optional.of(ordem));
        when(ordemRepository.save(any(OrdemDeServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrdemDeServicoDTO resultado = service.finalizarOsEntregue(OS_ID);

        assertThat(resultado.getStatus()).isEqualTo(StatusOS.ENTREGUE);
        assertThat(ordem.getStatusSaga()).isEqualTo(StatusSaga.FINALIZADA);
        assertThat(ordem.getDataFechamento()).isNotNull();
    }

    @Test
    void deveImpedirFinalizacaoDeOrdemQueNaoEstejaConcluida() {
        when(ordemRepository.existsByIdAndStatus(OS_ID, StatusOS.CONCLUIDA))
                .thenReturn(false);

        assertThatThrownBy(() -> service.finalizarOsEntregue(OS_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CONCLUIDA");
    }

    @Test
    void deveObterOrdemPorId() {
        OrdemDeServico ordem = criarOrdem(StatusOS.ABERTA);

        when(ordemRepository.findById(OS_ID))
                .thenReturn(Optional.of(ordem));

        OrdemDeServicoDTO resultado = service.obterOrdemPorId(OS_ID);

        assertThat(resultado.getId()).isEqualTo(OS_ID);
        assertThat(resultado.getNumero()).isEqualTo(ordem.getNumero());
    }

    @Test
    void deveListarOrdens() {
        when(ordemRepository.findAtivasOrdenadasPorPrioridade(
                List.of(StatusOS.CONCLUIDA, StatusOS.ENTREGUE)))
                .thenReturn(List.of(criarOrdem(StatusOS.ABERTA)));

        List<OrdemDeServicoDTO> resultado = service.listarOrdens();

        assertThat(resultado).hasSize(1);
    }

    @Test
    void deveListarOrdensPorCliente() {
        when(clienteRepository.existsById(CLIENTE_ID)).thenReturn(true);
        when(ordemRepository.findByVeiculoClienteId(CLIENTE_ID))
                .thenReturn(List.of(criarOrdem(StatusOS.ABERTA)));

        List<OrdemDeServicoDTO> resultado =
                service.listarOrdensPorCliente(CLIENTE_ID);

        assertThat(resultado).hasSize(1);
    }

    @Test
    void deveLancarExcecaoAoListarOrdensDeClienteInexistente() {
        when(clienteRepository.existsById(CLIENTE_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.listarOrdensPorCliente(CLIENTE_ID))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void deveObterStatusDaOrdem() {
        OrdemDeServico ordem = criarOrdem(StatusOS.EM_EXECUCAO);

        when(ordemRepository.findById(OS_ID))
                .thenReturn(Optional.of(ordem));

        OrdemStatusDTO resultado = service.obterStatus(OS_ID);

        assertThat(resultado.getStatus()).isEqualTo(StatusOS.EM_EXECUCAO);
    }

    // =========================================================
    // TEMPO MÉDIO DE EXECUÇÃO
    // =========================================================

    @Test
    void deveRetornarTempoMedioSemOrdensConcluidas() {
        LocalDate inicio = LocalDate.of(2026, 1, 1);
        LocalDate fim = LocalDate.of(2026, 1, 10);

        when(ordemRepository.findByDataExecucaoBetweenAndStatusEquals(
                any(), any(), eq(StatusOS.CONCLUIDA)))
                .thenReturn(Collections.emptyList());

        TempoExecucaoDTO resultado =
                service.listarTempoMedioPorPeriodo(inicio, fim);

        assertThat(resultado.getTempoMedio()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resultado.getQuantidadeOrdens()).isZero();
        assertThat(resultado.getDataInicio()).isEqualTo(inicio);
        assertThat(resultado.getDataFim()).isEqualTo(fim);
    }

    @Test
    void deveCalcularTempoMedioDeExecucao() {
        LocalDate inicio = LocalDate.of(2026, 1, 1);
        LocalDate fim = LocalDate.of(2026, 1, 10);

        OrdemDeServico primeira = criarOrdem(StatusOS.CONCLUIDA);
        primeira.setDataInicioExecucao(LocalDateTime.of(2026, 1, 1, 8, 0));
        primeira.setDataFechamento(LocalDateTime.of(2026, 1, 1, 10, 0));

        OrdemDeServico segunda = criarOrdem(StatusOS.CONCLUIDA);
        segunda.setDataInicioExecucao(LocalDateTime.of(2026, 1, 2, 8, 0));
        segunda.setDataFechamento(LocalDateTime.of(2026, 1, 2, 12, 0));

        when(ordemRepository.findByDataExecucaoBetweenAndStatusEquals(
                any(), any(), eq(StatusOS.CONCLUIDA)))
                .thenReturn(List.of(primeira, segunda));

        TempoExecucaoDTO resultado =
                service.listarTempoMedioPorPeriodo(inicio, fim);

        assertThat(resultado.getQuantidadeOrdens()).isEqualTo(2);
        assertThat(resultado.getTempoMedio()).isEqualByComparingTo("3");
    }

    @Test
    void deveUsarDataInicialQuandoDataFinalForNula() {
        LocalDate inicio = LocalDate.of(2026, 1, 1);

        when(ordemRepository.findByDataExecucaoBetweenAndStatusEquals(
                any(), any(), eq(StatusOS.CONCLUIDA)))
                .thenReturn(Collections.emptyList());

        TempoExecucaoDTO resultado =
                service.listarTempoMedioPorPeriodo(inicio, null);

        assertThat(resultado.getDataFim()).isEqualTo(inicio);
    }

    // =========================================================
    // AVANÇAR STATUS
    // =========================================================

    @Test
    void deveAvancarStatusEAtualizarObservacoes() {
        OrdemDeServico ordem = criarOrdem(StatusOS.ABERTA);

        AvancarStatusDTO dto = AvancarStatusDTO.builder()
                .novoStatus(StatusOS.EM_DIAGNOSTICO)
                .observacoes("Diagnóstico iniciado")
                .statusSaga(StatusSaga.ENVIADO_PARA_ORCAMENTO)
                .total(new BigDecimal("150.00"))
                .orcamentoId("ORC-01")
                .pagamentoId("PAG-01")
                .build();

        when(ordemRepository.findById(OS_ID))
                .thenReturn(Optional.of(ordem));
        when(ordemRepository.save(any(OrdemDeServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrdemDeServico resultado = service.avancarStatus(OS_ID, dto);

        assertThat(resultado.getStatus()).isEqualTo(StatusOS.EM_DIAGNOSTICO);
        assertThat(resultado.getObservacoes()).isEqualTo("Diagnóstico iniciado");
        assertThat(resultado.getValorTotal()).isEqualByComparingTo("150.00");
        assertThat(resultado.getStatusSaga())
                .isEqualTo(StatusSaga.ENVIADO_PARA_ORCAMENTO);
        assertThat(resultado.getOrcamentoId()).isEqualTo("ORC-01");
        assertThat(resultado.getPagamentoId()).isEqualTo("PAG-01");

        assertThat(meterRegistry.counter(
                "oficina.ordens.transicoes.status",
                "de", "ABERTA",
                "para", "EM_DIAGNOSTICO"
        ).count()).isEqualTo(1);
    }

    @Test
    void deveRegistrarDataInicioAoIniciarExecucao() {
        OrdemDeServico ordem = criarOrdem(StatusOS.APROVADA);

        when(ordemRepository.findById(OS_ID))
                .thenReturn(Optional.of(ordem));
        when(ordemRepository.save(any(OrdemDeServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.avancarStatus(OS_ID, new AvancarStatusDTO(StatusOS.EM_EXECUCAO));

        assertThat(ordem.getDataInicioExecucao()).isNotNull();
    }

    @Test
    void deveRegistrarDataFechamentoAoCancelar() {
        OrdemDeServico ordem = criarOrdem(StatusOS.ABERTA);

        when(ordemRepository.findById(OS_ID))
                .thenReturn(Optional.of(ordem));
        when(ordemRepository.save(any(OrdemDeServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.avancarStatus(OS_ID, new AvancarStatusDTO(StatusOS.CANCELADA));

        assertThat(ordem.getDataFechamento()).isNotNull();
    }

    @Test
    void devePermitirPermanecerNoMesmoStatus() {
        OrdemDeServico ordem = criarOrdem(StatusOS.ABERTA);

        when(ordemRepository.findById(OS_ID))
                .thenReturn(Optional.of(ordem));
        when(ordemRepository.save(any(OrdemDeServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.avancarStatus(OS_ID, new AvancarStatusDTO(StatusOS.ABERTA));

        assertThat(ordem.getStatus()).isEqualTo(StatusOS.ABERTA);
    }

    @ParameterizedTest
    @CsvSource({
            "ABERTA,EM_DIAGNOSTICO",
            "ABERTA,CANCELADA",
            "EM_DIAGNOSTICO,AGUARDANDO_APROVACAO",
            "EM_DIAGNOSTICO,CANCELADA",
            "AGUARDANDO_APROVACAO,APROVADA",
            "AGUARDANDO_APROVACAO,CANCELADA",
            "APROVADA,EM_EXECUCAO",
            "APROVADA,CANCELADA",
            "EM_EXECUCAO,CONCLUIDA",
            "EM_EXECUCAO,CANCELADA",
            "CONCLUIDA,ENTREGUE"
    })
    void devePermitirTransicoesValidas(String atual, String novo) {
        StatusOS statusAtual = StatusOS.valueOf(atual);
        StatusOS novoStatus = StatusOS.valueOf(novo);
        OrdemDeServico ordem = criarOrdem(statusAtual);

        when(ordemRepository.findById(OS_ID))
                .thenReturn(Optional.of(ordem));
        when(ordemRepository.save(any(OrdemDeServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() ->
                service.avancarStatus(OS_ID, new AvancarStatusDTO(novoStatus)))
                .doesNotThrowAnyException();

        assertThat(ordem.getStatus()).isEqualTo(novoStatus);
    }

    @ParameterizedTest
    @CsvSource({
            "ABERTA,APROVADA",
            "ABERTA,CONCLUIDA",
            "EM_DIAGNOSTICO,APROVADA",
            "AGUARDANDO_APROVACAO,EM_EXECUCAO",
            "APROVADA,CONCLUIDA",
            "EM_EXECUCAO,ENTREGUE",
            "CONCLUIDA,CANCELADA"
    })
    void deveImpedirTransicoesInvalidas(String atual, String novo) {
        OrdemDeServico ordem = criarOrdem(StatusOS.valueOf(atual));

        when(ordemRepository.findById(OS_ID))
                .thenReturn(Optional.of(ordem));

        assertThatThrownBy(() -> service.avancarStatus(
                OS_ID, new AvancarStatusDTO(StatusOS.valueOf(novo))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Transição de status inválida");

        verify(ordemRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = StatusOS.class, names = {"ENTREGUE", "CANCELADA"})
    void deveImpedirAlteracaoDeStatusTerminal(StatusOS status) {
        OrdemDeServico ordem = criarOrdem(status);

        when(ordemRepository.findById(OS_ID))
                .thenReturn(Optional.of(ordem));

        assertThatThrownBy(() -> service.avancarStatus(
                OS_ID, new AvancarStatusDTO(StatusOS.ABERTA)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("OS já finalizada");
    }

    @Test
    void deveAvancarStatusParaDTO() {
        OrdemDeServico ordem = criarOrdem(StatusOS.ABERTA);

        when(ordemRepository.findById(OS_ID))
                .thenReturn(Optional.of(ordem));
        when(ordemRepository.save(any(OrdemDeServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrdemDeServicoDTO resultado = service.avancarStatusDto(
                OS_ID, new AvancarStatusDTO(StatusOS.EM_DIAGNOSTICO));

        assertThat(resultado.getStatus()).isEqualTo(StatusOS.EM_DIAGNOSTICO);
    }

    // =========================================================
    // STATUS DA SAGA
    // =========================================================

    @Test
    void deveAtualizarStatusSaga() {
        OrdemDeServico ordem = criarOrdem(StatusOS.ABERTA);

        when(ordemRepository.findById(OS_ID))
                .thenReturn(Optional.of(ordem));
        when(ordemRepository.save(any(OrdemDeServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.atualizarStatusSaga(OS_ID, StatusSaga.FALHA_COMPENSACAO);

        assertThat(ordem.getStatusSaga())
                .isEqualTo(StatusSaga.FALHA_COMPENSACAO);
    }

    @Test
    void deveLancarExcecaoQuandoOrdemNaoExistirAoAtualizarSaga() {
        when(ordemRepository.findById(OS_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.atualizarStatusSaga(OS_ID, StatusSaga.COMPENSADA))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Ordem de serviço não encontrada");
    }

    // =========================================================
    // ADICIONAR ITENS
    // =========================================================

    @Test
    void deveAdicionarItemNaOrdem() {
        OrdemDeServico ordem = criarOrdem(StatusOS.ABERTA);
        Servico servico = criarServico();

        CatalogoResponseDTO estoque = new CatalogoResponseDTO(
                TipoItem.PECA,
                ITEM_ID,
                "Filtro de óleo",
                "Filtro",
                new BigDecimal("50.00"),
                true,
                10,
                30
        );

        AdicionarItemDTO dto = criarAdicionarItemDTO();

        when(ordemRepository.findById(OS_ID))
                .thenReturn(Optional.of(ordem));
        when(execucaoClient.getItem(TipoItem.SERVICO, SERVICO_ID))
                .thenReturn(new CatalogoResponseDTO(
                        TipoItem.SERVICO, SERVICO_ID, "Troca de óleo",
                        "Serviço", new BigDecimal("100.00"), true,
                        null, 60
                ));
        when(servicoRepository.findAll(any(org.springframework.data.domain.Example.class)))
                .thenReturn(List.of(servico));
        when(execucaoClient.getItem(TipoItem.PECA, ITEM_ID))
                .thenReturn(estoque);
        when(itemOSRepository.save(any(ItemOS.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(ordemRepository.save(any(OrdemDeServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrdemDeServicoDTO resultado = service.adicionarItens(OS_ID, dto);

        assertThat(resultado).isNotNull();
        assertThat(ordem.getStatus()).isEqualTo(StatusOS.EM_DIAGNOSTICO);
        assertThat(ordem.getItens()).hasSize(1);

        ItemOS itemSalvo = ordem.getItens().getFirst();
        assertThat(itemSalvo.getSubtotal()).isEqualByComparingTo("100.00");
        assertThat(itemSalvo.getTipoItem()).isEqualTo(TipoItem.PECA);
    }

    @Test
    void deveSalvarServicoQuandoAindaNaoExistirNoBanco() {
        OrdemDeServico ordem = criarOrdem(StatusOS.ABERTA);

        when(ordemRepository.findById(OS_ID))
                .thenReturn(Optional.of(ordem));
        when(execucaoClient.getItem(eq(TipoItem.SERVICO), eq(SERVICO_ID)))
                .thenReturn(new CatalogoResponseDTO(
                        TipoItem.SERVICO, SERVICO_ID, "Troca de óleo",
                        "Serviço", new BigDecimal("100.00"), true,
                        null, 60
                ));
        when(servicoRepository.findAll(any(org.springframework.data.domain.Example.class)))
                .thenReturn(Collections.emptyList());
        when(servicoRepository.save(any(Servico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(execucaoClient.getItem(eq(TipoItem.PECA), eq(ITEM_ID)))
                .thenReturn(new CatalogoResponseDTO(
                        TipoItem.PECA, ITEM_ID, "Filtro",
                        "Peça", new BigDecimal("50.00"), true,
                        10, 30
                ));
        when(itemOSRepository.save(any(ItemOS.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(ordemRepository.save(any(OrdemDeServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.adicionarItens(OS_ID, criarAdicionarItemDTO());

        verify(servicoRepository).save(any(Servico.class));
    }

    @ParameterizedTest
    @EnumSource(value = StatusOS.class, names = {
            "AGUARDANDO_APROVACAO",
            "APROVADA",
            "EM_EXECUCAO",
            "CONCLUIDA",
            "ENTREGUE",
            "CANCELADA"
    })
    void deveImpedirAdicionarItensEmStatusNaoPermitido(StatusOS status) {
        OrdemDeServico ordem = criarOrdem(status);

        when(ordemRepository.findById(OS_ID))
                .thenReturn(Optional.of(ordem));

        assertThatThrownBy(() ->
                service.adicionarItens(OS_ID, criarAdicionarItemDTO()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Não é possível adicionar itens");
    }

    @Test
    void deveConverterServicoNaoEncontradoEmNoSuchElementException() {
        OrdemDeServico ordem = criarOrdem(StatusOS.ABERTA);

        when(ordemRepository.findById(OS_ID))
                .thenReturn(Optional.of(ordem));
        when(execucaoClient.getItem(TipoItem.SERVICO, SERVICO_ID))
                .thenThrow(mock(FeignException.NotFound.class));

        assertThatThrownBy(() ->
                service.adicionarItens(OS_ID, criarAdicionarItemDTO()))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Serviço não encontrado");
    }

    @Test
    void deveConverterItemDeEstoqueNaoEncontradoEmNoSuchElementException() {
        OrdemDeServico ordem = criarOrdem(StatusOS.ABERTA);
        Servico servico = criarServico();

        when(ordemRepository.findById(OS_ID))
                .thenReturn(Optional.of(ordem));
        when(execucaoClient.getItem(TipoItem.SERVICO, SERVICO_ID))
                .thenReturn(new CatalogoResponseDTO(
                        TipoItem.SERVICO, SERVICO_ID, "Troca de óleo",
                        "Serviço", new BigDecimal("100.00"), true,
                        null, 60
                ));
        when(servicoRepository.findAll(any(org.springframework.data.domain.Example.class)))
                .thenReturn(List.of(servico));
        when(execucaoClient.getItem(TipoItem.PECA, ITEM_ID))
                .thenThrow(mock(FeignException.NotFound.class));

        assertThatThrownBy(() ->
                service.adicionarItens(OS_ID, criarAdicionarItemDTO()))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Item não encontrado");
    }

    // =========================================================
    // REMOVER ITENS
    // =========================================================

    @Test
    void deveRemoverItemDaOrdem() {
        OrdemDeServico ordem = criarOrdem(StatusOS.ABERTA);
        ItemOS item = criarItem(criarServico());
        ordem.getItens().add(item);

        when(ordemRepository.findById(OS_ID))
                .thenReturn(Optional.of(ordem));
        when(ordemRepository.save(any(OrdemDeServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrdemDeServicoDTO resultado = service.removerItem(OS_ID, ITEM_ID);

        assertThat(resultado).isNotNull();
        assertThat(ordem.getItens()).isEmpty();
    }

    @Test
    void deveLancarExcecaoQuandoItemNaoExistirNaOrdem() {
        OrdemDeServico ordem = criarOrdem(StatusOS.ABERTA);

        when(ordemRepository.findById(OS_ID))
                .thenReturn(Optional.of(ordem));

        assertThatThrownBy(() -> service.removerItem(OS_ID, ITEM_ID))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Item não encontrado");
    }

    @ParameterizedTest
    @EnumSource(value = StatusOS.class, names = {
            "AGUARDANDO_APROVACAO",
            "APROVADA",
            "EM_EXECUCAO",
            "CONCLUIDA",
            "ENTREGUE",
            "CANCELADA"
    })
    void deveImpedirRemoverItensEmStatusNaoPermitido(StatusOS status) {
        OrdemDeServico ordem = criarOrdem(status);

        when(ordemRepository.findById(OS_ID))
                .thenReturn(Optional.of(ordem));

        assertThatThrownBy(() -> service.removerItem(OS_ID, ITEM_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Não é possível remover itens");
    }

    // =========================================================
    // BUSCAR ENTIDADE
    // =========================================================

    @Test
    void deveBuscarEntidadePorId() {
        OrdemDeServico ordem = criarOrdem(StatusOS.ABERTA);

        when(ordemRepository.findById(OS_ID))
                .thenReturn(Optional.of(ordem));

        OrdemDeServico resultado = service.buscarEntidade(OS_ID);

        assertThat(resultado).isSameAs(ordem);
    }

    @Test
    void deveLancarExcecaoQuandoEntidadeNaoExistir() {
        when(ordemRepository.findById(OS_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarEntidade(OS_ID))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Ordem de serviço não encontrada");
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private Cliente criarCliente() {
        return Cliente.builder()
                .id(CLIENTE_ID)
                .nome("Cliente Teste")
                .cpfCnpj("12345678900")
                .email("teste@email.com")
                .telefone("11999999999")
                .ativo(true)
                .build();
    }

    private Veiculo criarVeiculo() {
        return Veiculo.builder()
                .id(VEICULO_ID)
                .placa("ABC1234")
                .marca("Fiat")
                .modelo("Uno")
                .ano(2020)
                .cliente(criarCliente())
                .build();
    }

    private OrdemDeServico criarOrdem(StatusOS status) {
        return OrdemDeServico.builder()
                .id(OS_ID)
                .numero("OS-2026-0001")
                .veiculo(criarVeiculo())
                .status(status)
                .dataAbertura(LocalDateTime.now().minusDays(1))
                .dataUltimaMudancaStatus(LocalDateTime.now().minusHours(1))
                .descricaoProblema("Problema de teste")
                .observacoes("Observação")
                .valorTotal(BigDecimal.ZERO)
                .itens(new ArrayList<>())
                .sagaId("SAGA-001")
                .statusSaga(StatusSaga.CRIADA)
                .build();
    }

    private Servico criarServico() {
        return Servico.builder()
                .id(1L)
                .idOrigem(SERVICO_ID)
                .nome("Troca de óleo")
                .descricao("Serviço de troca de óleo")
                .preco(new BigDecimal("100.00"))
                .tempoEstimadoMinutos(60)
                .tempoMedioExecucaoMinutos(null)
                .build();
    }

    private ItemOS criarItem(Servico servico) {
        return ItemOS.builder()
                .id(1L)
                .ordemDeServico(criarOrdem(StatusOS.ABERTA))
                .servico(servico)
                .quantidade(1)
                .precoUnitario(new BigDecimal("50.00"))
                .subtotal(new BigDecimal("50.00"))
                .nomeItem("Filtro de óleo")
                .idOrigemItem(ITEM_ID)
                .tipoItem(TipoItem.PECA)
                .build();
    }

    private AdicionarItemDTO criarAdicionarItemDTO() {
        AdicionarItemDTO.Item item = AdicionarItemDTO.Item.builder()
                .servicoId(SERVICO_ID)
                .itemEstoqueId(ITEM_ID)
                .tipoItem(TipoItem.PECA)
                .quantidade(2)
                .build();

        return new AdicionarItemDTO(List.of(item));
    }

    private void mockServicoEEstoque() {
        when(execucaoClient.getItem(TipoItem.SERVICO, SERVICO_ID))
                .thenReturn(new CatalogoResponseDTO(
                        TipoItem.SERVICO,
                        SERVICO_ID,
                        "Troca de óleo",
                        "Serviço",
                        new BigDecimal("100.00"),
                        true,
                        null,
                        60
                ));

        when(execucaoClient.getItem(TipoItem.PECA, ITEM_ID))
                .thenReturn(new CatalogoResponseDTO(
                        TipoItem.PECA,
                        ITEM_ID,
                        "Filtro de óleo",
                        "Peça",
                        new BigDecimal("50.00"),
                        true,
                        10,
                        30
                ));
    }
}
