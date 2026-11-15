package br.com.fiap.soat15.osservice.application.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteDTO {
    private Long id;

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    private String nome;

    @NotBlank(message = "CPF/CNPJ é obrigatório")
    @Pattern(regexp = "^([0-9]{11}|[A-Z0-9]{12}[0-9]{2})$", message = "CPF deve conter apenas números (11 dígitos) e CNPJ alfanumérico (14 dígitos)")
    private String cpfCnpj;

    @Email(message = "Email deve ser válido")
    private String email;

    @Pattern(regexp = "^[0-9]{8,9}$", message = "Telefone deve conter apenas números (8 a 9 dígitos)")
    private String telefone;

    @Size(max = 500, message = "Endereço deve ter no máximo 500 caracteres")
    private String endereco;

    private Boolean ativo;
}
