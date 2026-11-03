package br.com.fiap.soat15.osservice.application.usecase;

import br.com.fiap.soat15.osservice.adapter.out.gateway.ExecucaoClient;
import br.com.fiap.soat15.osservice.adapter.out.gateway.dto.ItemEstoqueResponseDTO;
import br.com.fiap.soat15.osservice.adapter.out.gateway.dto.ServicoResponseDTO;
import br.com.fiap.soat15.osservice.adapter.out.persistence.repository.*;
import br.com.fiap.soat15.osservice.application.dto.*;
import br.com.fiap.soat15.osservice.domain.entity.*;
import br.com.fiap.soat15.osservice.domain.exception.BusinessException;
import br.com.fiap.soat15.osservice.domain.service.OrdemDeServicoService;
import feign.FeignException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.EnumSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

@Service
@RequiredArgsConstructor
public class OrdemDeServicoServiceImpl implements OrdemDeServicoService {

    private static final Set<StatusOS> STATUS_PERMITE_ITENS = EnumSet.of(StatusOS.ABERTA, StatusOS.EM_DIAGNOSTICO);
    private static final Set<StatusOS> STATUS_TERMINAIS = EnumSet.of(StatusOS.ENTREGUE, StatusOS.CANCELADA);

    private final OrdemDeServicoRepository ordemRepository;
    private final ClienteRepository clienteRepository;
    private final VeiculoRepository veiculoRepository;
    private final ItemOSRepository itemOSRepository;
    private final ServicoRepository servicoRepository;
    private final MeterRegistry meterRegistry;

    @Autowired
    private final ExecucaoClient execucaoClient;

    @Override
    @Transactional
    public OrdemDeServicoDTO criarOrdem(CriarOrdemDTO dto) {
        Veiculo veiculo = veiculoRepository.findById(dto.getVeiculoId())
                .orElseThrow(() -> new NoSuchElementException("Veículo não encontrado: " + dto.getVeiculoId()));

        if (!veiculo.getCliente().getId().equals(dto.getClienteId())) {
            throw new BusinessException("Veículo não pertence ao cliente informado");
        }

        OrdemDeServico ordem = OrdemDeServico.builder()
                .numero(gerarNumero())
                .veiculo(veiculo)
                .status(StatusOS.ABERTA)
                .dataAbertura(LocalDateTime.now())
                .dataUltimaMudancaStatus(LocalDateTime.now())
                .descricaoProblema(dto.getDescricaoProblema())
                .observacoes(dto.getObservacoes())
                .valorTotal(BigDecimal.ZERO)
                .build();

        OrdemDeServico ordemSalva = ordemRepository.save(ordem);
        executarAposCommit(() -> meterRegistry.counter("oficina.ordens.criadas").increment());

        if (dto.getItensEstoqueCadastro() == null || dto.getItensEstoqueCadastro().isEmpty())
            return toDTO(ordemSalva);

        AdicionarItemDTO itemDTO = this.getAdicionarItemDTO(dto);

        return this.adicionarItens(ordemSalva.getId(), itemDTO);
    }

    public AdicionarItemDTO getAdicionarItemDTO(CriarOrdemDTO osDto) {

        List<AdicionarItemDTO.Item> itens = osDto.getItensEstoqueCadastro().stream()
                .map(itemCad -> AdicionarItemDTO.Item.builder()
                        .servicoId(osDto.getServicoId())
                        .itemEstoqueId(itemCad.getItemEstoqueId())
                        .quantidade(itemCad.getQuantidade())
                        .build())
                .toList();

        return new AdicionarItemDTO(itens);
    }


    @Override
    @Transactional(readOnly = true)
    public OrdemDeServicoDTO obterOrdemPorId(Long id) {
        return toDTO(buscarEntidade(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdemDeServicoDTO> listarOrdens() {
        return ordemRepository.findAtivasOrdenadasPorPrioridade(
                List.of(StatusOS.CONCLUIDA, StatusOS.ENTREGUE)
        ).stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdemDeServicoDTO> listarOrdensPorCliente(Long clienteId) {
        if (!clienteRepository.existsById(clienteId)) {
            throw new NoSuchElementException("Cliente não encontrado: " + clienteId);
        }
        return ordemRepository.findByVeiculoClienteId(clienteId).stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrdemStatusDTO obterStatus(Long id) {
        OrdemDeServico ordem = buscarEntidade(id);
        return OrdemStatusDTO.builder()
                .status(ordem.getStatus())
                .build();
    }

    @Override
    @Transactional
    public TempoExecucaoDTO listarTempoMedioPorPeriodo(LocalDate dataInicial, LocalDate dataFinal) {
        dataFinal = ofNullable(dataFinal).orElse(dataInicial);

        LocalDateTime dataFinalEndOfDay = dataFinal.plusDays(1).atStartOfDay();
        LocalDateTime dataInicialStartOfDay = dataInicial.atStartOfDay();

        List<OrdemDeServico> ordens = ordemRepository.findByDataExecucaoBetweenAndStatusEquals(
                dataInicialStartOfDay, dataFinalEndOfDay, StatusOS.CONCLUIDA);

        int qtdOrdens = 0;
        BigDecimal averageTime = BigDecimal.ZERO;

        if (nonNull(ordens) && !ordens.isEmpty()) {
            BigDecimal timeExecutions = BigDecimal.valueOf(ordens.stream()
                    .mapToLong(o -> Duration.between(
                            o.getDataInicioExecucao(),
                            o.getDataFechamento()
                    ).toHours())
                    .sum());

            qtdOrdens = ordens.size();
            averageTime = timeExecutions.divide(
                    BigDecimal.valueOf(qtdOrdens), RoundingMode.HALF_DOWN);
        }

        return TempoExecucaoDTO.builder()
                .tempoMedio(averageTime)
                .quantidadeOrdens(qtdOrdens)
                .dataInicio(dataInicial)
                .dataFim(dataFinal)
                .build();
    }

    @Override
    @Transactional
    public OrdemDeServicoDTO avancarStatus(Long id, AvancarStatusDTO dto) {
        OrdemDeServico ordem = buscarEntidade(id);
        StatusOS statusAtual = ordem.getStatus();
        validarTransicao(ordem.getStatus(), dto.getNovoStatus());
        LocalDateTime momentoTransicao = LocalDateTime.now();

        registrarTempoNoStatusAnterior(ordem, statusAtual, momentoTransicao);

        ordem.setStatus(dto.getNovoStatus());
        ordem.setDataUltimaMudancaStatus(momentoTransicao);
        executarAposCommit(() -> meterRegistry.counter(
                    "oficina.ordens.transicoes.status",
                    Tags.of("de", statusAtual.name(), "para", dto.getNovoStatus().name())
            ).increment());

        if (dto.getObservacoes() != null && !dto.getObservacoes().isBlank()) {
            ordem.setObservacoes(dto.getObservacoes());
        }

        if (dto.getNovoStatus() == StatusOS.EM_EXECUCAO) {
            ordem.setDataInicioExecucao(momentoTransicao);
        }

        if (dto.getNovoStatus() == StatusOS.CONCLUIDA) {
            recalcularTempoMedioServicos(ordem);
            registrarTempoExecucao(ordem, momentoTransicao);
        } else if (dto.getNovoStatus() == StatusOS.ENTREGUE) {
            ordem.setDataFechamento(momentoTransicao);
        } else if (dto.getNovoStatus() == StatusOS.CANCELADA) {
            ordem.setDataFechamento(momentoTransicao);
        }

        return toDTO(ordemRepository.save(ordem));
    }

    @Override
    @Transactional
    public OrdemDeServicoDTO adicionarItens(Long ordemId, AdicionarItemDTO dto) {
        OrdemDeServico ordem = buscarEntidade(ordemId);

        if (!STATUS_PERMITE_ITENS.contains(ordem.getStatus())) {
            throw new IllegalArgumentException(
                    "Não é possível adicionar itens à OS no status: " + ordem.getStatus());
        }

        for (AdicionarItemDTO.Item itemDTO : dto.getItens()) {
            Servico servico = this.getServico(itemDTO.getServicoId());

            ItemEstoqueResponseDTO itemEstoque = getItemEstoque(itemDTO.getItemEstoqueId());

            BigDecimal precoUnitario = itemEstoque.precoUnitario();
            BigDecimal subtotal = precoUnitario.multiply(BigDecimal.valueOf(itemDTO.getQuantidade()));

            /// itemEstoque.reduzirEstoque(itemDTO.getQuantidade()); ---> CHAMAR FILA ESTOQUE

            ItemOS item = ItemOS.builder()
                    .ordemDeServico(ordem)
                    .servico(servico)
                    .nomeItem(itemEstoque.nome())
                    .idOrigemItem(itemEstoque.id())
                    .quantidade(itemDTO.getQuantidade())
                    .precoUnitario(precoUnitario)
                    .subtotal(subtotal)
                    .build();

            ItemOS itemSalvo = itemOSRepository.save(item);
            ordem.getItens().add(itemSalvo);
        }

        recalcularTotal(ordem);
        return toDTO(ordemRepository.save(ordem));
    }

    @Override
    @Transactional
    public OrdemDeServicoDTO removerItem(Long ordemId, Long itemId) {
        OrdemDeServico ordem = buscarEntidade(ordemId);

        if (!STATUS_PERMITE_ITENS.contains(ordem.getStatus())) {
            throw new IllegalArgumentException(
                    "Não é possível remover itens da OS no status: " + ordem.getStatus());
        }

        ItemOS item = ordem.getItens().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Item não encontrado: " + itemId));

//        Item itemEstoque = item.getItem();
        /// itemEstoque.aumentarEstoque(item.getQuantidade()); ---- CHAMAR FILA ESTOQUE
//        itemEstoqueRepository.save(itemEstoque);

        ordem.getItens().remove(item);
        recalcularTotal(ordem);

        return toDTO(ordemRepository.save(ordem));
    }

    private Servico getServico(Long idServico) {
        try {
            ServicoResponseDTO dto = execucaoClient.buscarServicoPorId(idServico);

            Servico servico = new Servico(dto);

            // Configura o Example para ignorar o ID (que é nulo ou gerado) e propriedades nulas
            ExampleMatcher matcher = ExampleMatcher.matching()
                    .withIgnorePaths("id") // Ignora o ID na comparação
                    .withIgnoreCase();      // Opcional: ignora maiúsculas/minúsculas para strings

            Example<Servico> example = Example.of(servico, matcher);

            // Executa a busca
            List<Servico> resultados = servicoRepository.findAll(example);

            if (!resultados.isEmpty())
                return resultados.getFirst();

            return servicoRepository.save(servico);

        } catch (FeignException.NotFound e) {
            throw new NoSuchElementException("Serviço não encontrado: " + idServico);
        }
    }

    private ItemEstoqueResponseDTO getItemEstoque(Long idItem) {
        try {
            return execucaoClient.buscarItemPorId(idItem);
        } catch (FeignException.NotFound e) {
            throw new NoSuchElementException("Item não encontrado: " + idItem);
        }
    }

    private OrdemDeServico buscarEntidade(Long id) {
        return ordemRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Ordem de serviço não encontrada: " + id));
    }

    private void validarTransicao(StatusOS atual, StatusOS novo) {
        if (STATUS_TERMINAIS.contains(atual)) {
            throw new BusinessException(
                    "Não é possível alterar o status de uma OS já finalizada: " + atual);
        }

        boolean valido = switch (atual) {
            case ABERTA -> novo == StatusOS.EM_DIAGNOSTICO || novo == StatusOS.CANCELADA;
            case EM_DIAGNOSTICO -> novo == StatusOS.AGUARDANDO_APROVACAO || novo == StatusOS.CANCELADA;
            case AGUARDANDO_APROVACAO -> novo == StatusOS.APROVADA || novo == StatusOS.CANCELADA;
            case APROVADA -> novo == StatusOS.EM_EXECUCAO || novo == StatusOS.CANCELADA;
            case EM_EXECUCAO -> novo == StatusOS.CONCLUIDA;
            case CONCLUIDA -> novo == StatusOS.ENTREGUE;
            default -> false;
        };

        if (!valido) {
            throw new IllegalArgumentException(
                    "Transição de status inválida: " + atual + " → " + novo);
        }
    }

    private void recalcularTempoMedioServicos(OrdemDeServico ordem) {
        if (ordem.getDataInicioExecucao() == null || ordem.getItens().isEmpty()) {
            return;
        }

        long duracaoMinutos = Duration.between(ordem.getDataInicioExecucao(), LocalDateTime.now()).toMinutes();
        if (duracaoMinutos <= 0) {
            return;
        }

        int tempoExecucao = (int) duracaoMinutos;

        for (ItemOS item : ordem.getItens()) {
            Servico servico = item.getServico();
            if (servico.getTempoMedioExecucaoMinutos() == null) {
                servico.setTempoMedioExecucaoMinutos(tempoExecucao);
            } else {
                servico.setTempoMedioExecucaoMinutos(
                        (servico.getTempoMedioExecucaoMinutos() + tempoExecucao) / 2
                );
            }
            servicoRepository.save(servico);
        }
    }

    private void recalcularTotal(OrdemDeServico ordem) {
        BigDecimal total = ordem.getItens().stream()
                .map(ItemOS::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        ordem.setValorTotal(total);
    }

    private void registrarTempoNoStatusAnterior(OrdemDeServico ordem, StatusOS statusAtual, LocalDateTime momentoTransicao) {
        LocalDateTime inicioStatus = ofNullable(ordem.getDataUltimaMudancaStatus()).orElse(ordem.getDataAbertura());
        Duration tempoNoStatus = Duration.between(inicioStatus, momentoTransicao);
        if (tempoNoStatus.isZero() || tempoNoStatus.isNegative()) {
            return;
        }

        executarAposCommit(() -> meterRegistry.timer(
                    "oficina.ordens.status.duracao",
                    Tags.of("status", statusAtual.name())
            ).record(tempoNoStatus));
    }

    private void registrarTempoExecucao(OrdemDeServico ordem, LocalDateTime momentoFinalizacao) {
        if (ordem.getDataInicioExecucao() == null) {
            return;
        }

        Duration duracaoExecucao = Duration.between(ordem.getDataInicioExecucao(), momentoFinalizacao);
        if (duracaoExecucao.isZero() || duracaoExecucao.isNegative()) {
            return;
        }

        executarAposCommit(() -> meterRegistry.timer("oficina.ordens.execucao.duracao").record(duracaoExecucao));
    }

    private void executarAposCommit(Runnable registroMetrica) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            registroMetrica.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                registroMetrica.run();
            }
        });
    }

    private String gerarNumero() {
        int ano = Year.now().getValue();
        long count = ordemRepository.count() + 1;
        return String.format("OS-%d-%04d", ano, count);
    }

    private OrdemDeServicoDTO toDTO(OrdemDeServico ordem) {
        List<ItemOSDTO> itensDTO = ordem.getItens().stream().map(this::toItemDTO).toList();

        return OrdemDeServicoDTO.builder()
                .id(ordem.getId())
                .numero(ordem.getNumero())
                .status(ordem.getStatus())
                .clienteId(ordem.getVeiculo().getCliente().getId())
                .clienteNome(ordem.getVeiculo().getCliente().getNome())
                .veiculoId(ordem.getVeiculo().getId())
                .veiculoPlaca(ordem.getVeiculo().getPlaca())
                .veiculoModelo(ordem.getVeiculo().getModelo())
                .dataAbertura(ordem.getDataAbertura())
                .dataInicioExecucao(ordem.getDataInicioExecucao())
                .dataFechamento(ordem.getDataFechamento())
                .descricaoProblema(ordem.getDescricaoProblema())
                .observacoes(ordem.getObservacoes())
                .valorTotal(ordem.getValorTotal())
                .itens(itensDTO)
                .build();
    }

    private ItemOSDTO toItemDTO(ItemOS item) {
        return ItemOSDTO.builder()
                .id(item.getId())
                .servicoId(item.getServico().getIdOrigem())
                .servicoNome(item.getServico().getNome())
                .itemEstoqueId(item.getIdOrigemItem())
                .itemEstoqueNome(item.getNomeItem())
                .quantidade(item.getQuantidade())
                .precoUnitario(item.getPrecoUnitario())
                .subtotal(item.getSubtotal())
                .build();
    }
}
