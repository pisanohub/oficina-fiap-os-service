package br.com.fiap.soat15.osservice.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "TB_ORDENS_SERVICO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdemDeServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String numero;

    @ManyToOne()
    @JoinColumn(name = "veiculo_id", nullable = false)
    private Veiculo veiculo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusOS status;

    @Column(nullable = false)
    private LocalDateTime dataAbertura;

    @Column
    private LocalDateTime dataUltimaMudancaStatus;

    @Column
    private LocalDateTime dataInicioExecucao;

    @Column
    private LocalDateTime dataFechamento;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String descricaoProblema;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @OneToMany(mappedBy = "ordemDeServico", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItemOS> itens = new ArrayList<>();

    @Column()
    private String sagaId;

    @Column()
    private String orcamentoId;

    @Column()
    private String pagamentoId;

    @Version
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_saga", nullable = false)
    private StatusSaga statusSaga;

//    @OneToMany(mappedBy = "ordemDeServico", cascade = CascadeType.ALL, orphanRemoval = true)
//    @OrderBy("dataHora ASC")
//    @Builder.Default
//    private List<OsHistoricoStatus> historico = new ArrayList<>();
}
