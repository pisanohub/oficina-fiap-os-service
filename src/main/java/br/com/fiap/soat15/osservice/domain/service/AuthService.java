package br.com.fiap.soat15.osservice.domain.service;

import br.com.fiap.soat15.osservice.application.dto.LoginRequest;
import br.com.fiap.soat15.osservice.application.dto.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    void registro(LoginRequest request);
}
