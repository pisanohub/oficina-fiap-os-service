package br.com.fiap.soat15.osservice.domain.service;

import br.com.fiap.soat15.osservice.application.dto.*;
import br.com.fiap.soat15.osservice.domain.entity.OrdemDeServico;
import br.com.fiap.soat15.osservice.domain.entity.StatusOS;
import br.com.fiap.soat15.osservice.domain.entity.StatusOrcamento;
import br.com.fiap.soat15.osservice.domain.entity.StatusSaga;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface OrdemDeServicoService {

    OrdemDeServicoDTO criarOrdem(CriarOrdemDTO dto);

    void cancelarOrdem(Long id);

    OrdemDeServicoDTO obterOrdemPorId(Long id);

    OrdemDeServico buscarEntidade(Long id);

    OrdemStatusDTO obterStatus(Long id);

    List<OrdemDeServicoDTO> listarOrdens();

    List<OrdemDeServicoDTO> listarOrdensPorCliente(Long clienteId);

    TempoExecucaoDTO listarTempoMedioPorPeriodo(LocalDate dataInicial, LocalDate dataFinal);

    OrdemDeServicoDTO avancarStatusDto(Long id, AvancarStatusDTO dto);

    OrdemDeServico avancarStatus(Long id, AvancarStatusDTO dto);

    void atualizarStatusSaga(Long id, StatusSaga novoStatus);

    OrdemDeServicoDTO adicionarItens(Long ordemId, AdicionarItemDTO dto);

    OrdemDeServicoDTO removerItem(Long ordemId, String itemId);

    OrdemDeServicoDTO solicitarOrcamento(Long id);

    List<OrdemDeServicoDTO> listarOrcamentosAbertosPorCliente(Long clientId);

    OrdemDeServicoDTO aprovarOrcamento(Long id, StatusOrcamento statusOrcamento);

    List<OrdemDeServicoDTO> listarOsConcluidasPorCliente(Long clientId);

    OrdemDeServicoDTO finalizarOsEntregue(Long id);
}
