package br.com.fiap.soat15.osservice.adapter.out.gateway.dto;

import br.com.fiap.soat15.osservice.domain.entity.TipoItem;

import java.math.BigDecimal;

public record CatalogoResponseDTO(
        TipoItem tipo,
        String itemId,
        String nome,
        String descricao,
        BigDecimal precoUnitario,
        boolean ativo,
        Integer quantidadeEstoque,
        Integer tempoEstimadoMinutos
) { }