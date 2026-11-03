package br.com.fiap.soat15.osservice.domain.entity;

import br.com.fiap.soat15.osservice.adapter.out.gateway.dto.ItemEstoqueResponseDTO;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "TB_ITENS")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Item {

    public Item(ItemEstoqueResponseDTO dto) {
        this.idOrigem = dto.id();
        this.nome = dto.nome();
        this.descricao = dto.descricao();
        this.precoUnitario = dto.precoUnitario();
        this.tipo = dto.tipo();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long idOrigem;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(length = 300)
    private String descricao;

    @Column(nullable = false)
    private BigDecimal precoUnitario;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoItem tipo;

    public BigDecimal calcularCustoTotal(int quantidade) {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }
}
