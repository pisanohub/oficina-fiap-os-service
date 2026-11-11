package br.com.fiap.soat15.osservice.adapter.out.saga.dto;


import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record ExecucaoResultadoEventDto(
        @JsonUnwrapped
        EnvelopeResponseSQS envelopeResponseSQS,
        PayloadResLinhaDto payload
) {
    public record PayloadResLinhaDto(
            String status,
            String iniciadaEm,
            String concluidaEm,
            String orcamentoId,
            int versao,
            String pagamentoId,
            String motivo, // Ex: "ESTOQUE_INSUFICIENTE"
            String itemId
    ) {}
}