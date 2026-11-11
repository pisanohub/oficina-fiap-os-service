package br.com.fiap.soat15.osservice.application.dto;

import br.com.fiap.soat15.osservice.domain.entity.TipoItem;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItensEstoqueCadastro {
    private String itemEstoqueId;
    private TipoItem tipoItem;
    private Integer quantidade;
}