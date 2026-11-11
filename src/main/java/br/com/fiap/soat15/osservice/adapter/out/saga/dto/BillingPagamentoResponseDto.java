package br.com.fiap.soat15.osservice.adapter.out.saga.dto;

import java.math.BigDecimal;

public record BillingPagamentoResponseDto(
        String id,
        String messageId,
        String sagaId,
        String causationId,
        String comandoId,
        Long ordemId,
        Long clienteId,
        String status,
        String moeda,
        BigDecimal total,
        String criadoEm
){ }