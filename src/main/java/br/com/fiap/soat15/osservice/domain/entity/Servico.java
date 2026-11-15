package br.com.fiap.soat15.osservice.domain.entity;

import br.com.fiap.soat15.osservice.adapter.out.gateway.dto.CatalogoResponseDTO;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "TB_SERVICOS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Servico {

    public Servico(CatalogoResponseDTO catalogoDTO){
        this.idOrigem = catalogoDTO.itemId();
        this.nome = catalogoDTO.nome();
        this.descricao = catalogoDTO.descricao();
        this.preco = catalogoDTO.precoUnitario();
        this.tempoEstimadoMinutos = catalogoDTO.tempoEstimadoMinutos();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String idOrigem;

    @Column(nullable = false, unique = true, length = 100)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(nullable = false)
    private Integer tempoEstimadoMinutos;

    @Column
    private Integer tempoMedioExecucaoMinutos;

}
