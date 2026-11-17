package br.com.fiap.soat15.osservice.adapter.out.saga.dto;

import java.time.Instant;
import java.util.List;

public record AgendarExecucaoCommandDto(
        int schemaVersion,
        String messageId,
        String operationId,
        String type,
        String sagaId,
        String osId,
        String causationId,
        Instant occurredAt,
        PayloadDto payload
) {
    public record PayloadDto(
            String orcamentoId,
            int versao,
            String pagamentoId,
            List<ItemCommandDto> itens
    ) {}

    public record ItemCommandDto(
            String tipo, // Ex: "PECA" ou "SERVICO"[cite: 3]
            String itemId,
            int quantidade
    ) {}
}
