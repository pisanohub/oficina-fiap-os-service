package br.com.fiap.soat15.osservice.application.dto;

import br.com.fiap.soat15.osservice.domain.entity.StatusOS;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvancarStatusDTO {

    @NotNull(message = "Novo status é obrigatório")
    private StatusOS novoStatus;

    private String observacoes;
}
