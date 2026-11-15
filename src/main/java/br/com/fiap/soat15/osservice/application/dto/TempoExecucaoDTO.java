package br.com.fiap.soat15.osservice.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TempoExecucaoDTO {
    private BigDecimal tempoMedio;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private Integer quantidadeOrdens;
}
