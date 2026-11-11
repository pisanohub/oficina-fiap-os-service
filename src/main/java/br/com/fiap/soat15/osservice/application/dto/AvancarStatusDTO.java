package br.com.fiap.soat15.osservice.application.dto;

import br.com.fiap.soat15.osservice.domain.entity.StatusOS;
import br.com.fiap.soat15.osservice.domain.entity.StatusSaga;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvancarStatusDTO {

    public AvancarStatusDTO(StatusOS novoStatus) {
        this.novoStatus = novoStatus;
    }

    @NotNull(message = "Novo status é obrigatório")
    private StatusOS novoStatus;

    private String observacoes;

    private BigDecimal total;

    private StatusSaga statusSaga;

    private String pagamentoId;

    private String orcamentoId;
}
