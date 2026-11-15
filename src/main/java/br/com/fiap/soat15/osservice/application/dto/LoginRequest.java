package br.com.fiap.soat15.osservice.application.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
    private String cpf;
}
