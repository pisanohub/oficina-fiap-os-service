package br.com.fiap.soat15.osservice.adapter.out.saga.dto;

import java.time.Instant;

public record EnvelopeResponseSQS (
        int schemaVersion,
        String messageId,
        String operationId,
        String type,
        String sagaId,
        String osId,
        String causationId,
        Instant occurredAt
){}
