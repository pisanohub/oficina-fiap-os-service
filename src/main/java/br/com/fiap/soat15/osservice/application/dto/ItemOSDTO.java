package br.com.fiap.soat15.osservice.application.dto;

import br.com.fiap.soat15.osservice.domain.entity.TipoItem;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemOSDTO {

    private Long id;
    private TipoItem tipoItem;
    private String servicoId;
    private String itemEstoqueId;
    private String itemEstoqueNome;
    private String servicoNome;
    private Integer quantidade;
    private BigDecimal precoUnitario;
    private BigDecimal subtotal;
}
