package br.com.fiap.soat15.osservice.domain.service;

import br.com.fiap.soat15.osservice.adapter.out.saga.dto.BillingResponse;

public interface AgendaExecucaoService {
    void executar(BillingResponse.PagamentoResponseDto pagamentoResponseDto);
}
