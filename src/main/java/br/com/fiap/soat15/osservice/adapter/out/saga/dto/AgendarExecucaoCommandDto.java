package br.com.fiap.soat15.osservice.adapter.out.saga.dto;

import java.util.List;

public record AgendarExecucaoCommandDto(
        EnvelopeResponseSQS envelopeResponseSQS,
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