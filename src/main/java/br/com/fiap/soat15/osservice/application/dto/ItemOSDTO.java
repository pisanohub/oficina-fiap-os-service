package br.com.fiap.soat15.osservice.application.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemOSDTO {

    private Long id;
    private Long servicoId;
    private Long itemEstoqueId;
    private String itemEstoqueNome;
    private String servicoNome;
    private Integer quantidade;
    private BigDecimal precoUnitario;
    private BigDecimal subtotal;
}
