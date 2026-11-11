package br.com.fiap.soat15.osservice.domain.service;

import br.com.fiap.soat15.osservice.domain.entity.OrdemDeServico;

public interface OrcamentoService {
    void solicitarOrcamento(OrdemDeServico ordem, Long clienteId);

    void processarPagamento(OrdemDeServico ordem, Long clienteId);
}
