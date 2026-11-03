package br.com.fiap.soat15.osservice.config.security;

import java.security.Principal;

public record ClientePrincipal(Long clienteId, String cpf) implements Principal {
    @Override
    public String getName() {
        return cpf;
    }
}
