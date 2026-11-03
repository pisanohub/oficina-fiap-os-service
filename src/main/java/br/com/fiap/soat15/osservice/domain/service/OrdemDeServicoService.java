package br.com.fiap.soat15.osservice.domain.service;

import br.com.fiap.soat15.osservice.application.dto.*;

import java.time.LocalDate;
import java.util.List;

public interface OrdemDeServicoService {

    OrdemDeServicoDTO criarOrdem(CriarOrdemDTO dto);

    OrdemDeServicoDTO obterOrdemPorId(Long id);

    OrdemStatusDTO obterStatus(Long id);

    List<OrdemDeServicoDTO> listarOrdens();

    List<OrdemDeServicoDTO> listarOrdensPorCliente(Long clienteId);

    TempoExecucaoDTO listarTempoMedioPorPeriodo(LocalDate dataInicial, LocalDate dataFinal);

    OrdemDeServicoDTO avancarStatus(Long id, AvancarStatusDTO dto);

    OrdemDeServicoDTO adicionarItens(Long ordemId, AdicionarItemDTO dto);

    OrdemDeServicoDTO removerItem(Long ordemId, Long itemId);

}
