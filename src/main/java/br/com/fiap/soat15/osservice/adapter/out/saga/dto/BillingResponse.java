package br.com.fiap.soat15.osservice.adapter.out.saga.dto;

import br.com.fiap.soat15.osservice.domain.entity.StatusBilling;

import java.math.BigDecimal;
import java.util.List;

public class BillingResponse {

    public sealed interface MessageDto permits OrcamentoResponseDto, PagamentoResponseDto {
        String id();
        String messageId();
        String sagaId();
        String causationId();
        String comandoId();
        Long ordemId();
        Long clienteId();
        StatusBilling status();
    }
    public record OrcamentoResponseDto(
            String id,
            String messageId,
            String sagaId,
            String causationId,
            String comandoId,
            Long ordemId,
            Long clienteId,
            StatusBilling status,
            String moeda,
            BigDecimal total,
            String criadoEm,
            List<ItensOrcamento> itens
    ) implements MessageDto {}

    public record ItensOrcamento(
            String referencia,
            String tipo,
            String descricao,
            Integer quantidade,
            BigDecimal valorUnitario
    ) {}

    public record PagamentoResponseDto(
            String id,
            String messageId,
            String sagaId,
            String causationId,
            String comandoId,
            Long ordemId,
            Long clienteId,
            String orcamentoId,
            String pagamentoId,
            StatusBilling status,
            String moeda,
            BigDecimal valor,
            Integer versao
    ) implements MessageDto{}
}

