package br.com.fiap.soat15.osservice.application.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class TempoExecucaoDTO {
    private BigDecimal tempoMedio;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private Integer quantidadeOrdens;
}
