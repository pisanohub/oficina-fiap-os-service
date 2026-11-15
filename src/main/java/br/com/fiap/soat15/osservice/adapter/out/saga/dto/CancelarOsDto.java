package br.com.fiap.soat15.osservice.adapter.out.saga.dto;

import br.com.fiap.soat15.osservice.domain.entity.OrdemDeServico;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class CancelarOsDto {
    public CancelarOsDto(OrdemDeServico ordemDeServico, String type) {
        this.ordemDeServico = ordemDeServico;
        this.type = type;
    }

    private OrdemDeServico ordemDeServico;
    private String type;
    private String causationId;
}
