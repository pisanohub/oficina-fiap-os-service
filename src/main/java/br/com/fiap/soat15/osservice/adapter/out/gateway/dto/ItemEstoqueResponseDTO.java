package br.com.fiap.soat15.osservice.adapter.out.gateway.dto;

import br.com.fiap.soat15.osservice.domain.entity.TipoItem;

import java.math.BigDecimal;

public record ItemEstoqueResponseDTO(Long id, String nome, String descricao, Integer quantidadeEstoque,
                                     BigDecimal precoUnitario, TipoItem tipo, Boolean ativo) {
}
