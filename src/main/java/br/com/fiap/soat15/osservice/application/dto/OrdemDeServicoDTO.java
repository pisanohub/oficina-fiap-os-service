package br.com.fiap.soat15.osservice.application.dto;

import br.com.fiap.soat15.osservice.domain.entity.StatusOS;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdemDeServicoDTO {

    private Long id;
    private String numero;
    private StatusOS status;
    private Long clienteId;
    private String clienteNome;
    private Long veiculoId;
    private String veiculoPlaca;
    private String veiculoModelo;
    private LocalDateTime dataAbertura;
    private LocalDateTime dataInicioExecucao;
    private LocalDateTime dataFechamento;
    private String descricaoProblema;
    private String observacoes;
    private BigDecimal valorTotal;
    private List<ItemOSDTO> itens;
    private String sagaId;
}
