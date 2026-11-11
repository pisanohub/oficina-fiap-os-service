package br.com.fiap.soat15.osservice.application.dto;

import br.com.fiap.soat15.osservice.domain.entity.TipoItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdicionarItemDTO {

    @NotEmpty(message = "A lista de itens não pode ser vazia")
    @Valid
    private List<Item> itens;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Item {

        @NotNull(message = "servicoId é obrigatório")
        private String servicoId;

        @NotNull(message = "itemEstoqueId é obrigatório")
        private String itemEstoqueId;

        @NotNull(message = "tipoItem é obrigatório")
        private TipoItem tipoItem;

        @NotNull(message = "Quantidade é obrigatória")
        @Min(value = 1, message = "Quantidade deve ser pelo menos 1")
        private Integer quantidade;
    }
}
