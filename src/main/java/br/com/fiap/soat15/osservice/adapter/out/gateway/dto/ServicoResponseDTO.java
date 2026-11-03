package br.com.fiap.soat15.osservice.adapter.out.gateway.dto;

import java.math.BigDecimal;

public record ServicoResponseDTO(Long id, String nome, String descricao, BigDecimal preco, Integer tempoEstimadoMinutos,
                                 Integer tempoMedioExecucaoMinutos, Boolean ativo) {
}
