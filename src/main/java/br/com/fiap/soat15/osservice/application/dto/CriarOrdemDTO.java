package br.com.fiap.soat15.osservice.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CriarOrdemDTO {

    public CriarOrdemDTO(Long clienteId, Long veiculoId, String descricaoProblema) {
        this.clienteId = clienteId;
        this.veiculoId = veiculoId;
        this.descricaoProblema = descricaoProblema;
    }

    @NotNull(message = "clienteId é obrigatório")
    private Long clienteId;

    @NotNull(message = "veiculoId é obrigatório")
    private Long veiculoId;

    private String servicoId;

    private List<ItensEstoqueCadastro> itensEstoqueCadastro;

    @NotBlank(message = "Descrição do problema é obrigatória")
    private String descricaoProblema;

    private String observacoes;
}
