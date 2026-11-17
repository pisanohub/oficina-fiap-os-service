package br.com.fiap.soat15.osservice.adapter.out.saga.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CompensacaoDto {
    public CompensacaoDto(String osId, String type) {
        this.osId = osId;
        this.type = type;
    }

    @NotNull
    private String osId;
    @NotNull
    private String type;
    private EnvelopeResponseSQS envelopeResponseSQS;
    private String motivo;
}
