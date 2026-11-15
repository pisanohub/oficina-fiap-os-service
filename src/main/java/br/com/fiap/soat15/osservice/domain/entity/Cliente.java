package br.com.fiap.soat15.osservice.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TB_CLIENTES")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, unique = true, length = 20)
    private String cpfCnpj;

    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String telefone;

    @Column(columnDefinition = "TEXT")
    private String endereco;

    @Column(columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean ativo = true;
}
