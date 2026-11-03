//package br.com.fiap.soat15.osservice.domain.entity;
//
//import jakarta.persistence.*;
//import lombok.*;
//
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "TB_OS_HISTORICO_STATUS")
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class OsHistoricoStatus {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @ManyToOne(fetch = FetchType.LAZY, optional = false)
//    @JoinColumn(name = "ordem_servico_id", nullable = false)
//    private OrdemDeServico ordemDeServico;
//
//    @Enumerated(EnumType.STRING)
//    @Column(length = 30)
//    private StatusOS statusAnterior;
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false, length = 30)
//    private StatusOS statusNovo;
//
//    @Column(nullable = false)
//    private LocalDateTime dataHora;
//
//    @Column(length = 255)
//    private String motivo;
//}
