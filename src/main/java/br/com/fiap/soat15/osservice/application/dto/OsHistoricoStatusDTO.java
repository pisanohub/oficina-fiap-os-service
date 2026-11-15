package br.com.fiap.soat15.osservice.application.dto;

import br.com.fiap.soat15.osservice.domain.entity.StatusOS;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OsHistoricoStatusDTO {
    private Long id;
    private StatusOS statusAnterior;
    private StatusOS statusNovo;
    private LocalDateTime dataHora;
    private String motivo;
}
