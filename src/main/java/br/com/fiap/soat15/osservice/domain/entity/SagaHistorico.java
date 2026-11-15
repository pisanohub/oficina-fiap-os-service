package br.com.fiap.soat15.osservice.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "TB_SAGA_HISTORICO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SagaHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String messageId;
    @Column
    private String operationId;
    @Column
    private String type;
    @Column
    private String sagaId;
    @Column
    private String osId;
    @Column
    private String causationId;
    @Column
    private Instant occurredAt;
}
