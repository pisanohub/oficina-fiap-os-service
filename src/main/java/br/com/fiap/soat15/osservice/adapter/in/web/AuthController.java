package br.com.fiap.soat15.osservice.adapter.in.web;

import br.com.fiap.soat15.osservice.application.dto.LoginRequest;
import br.com.fiap.soat15.osservice.application.dto.LoginResponse;
import br.com.fiap.soat15.osservice.domain.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@AllArgsConstructor
@Tag(name = "Autenticação", description = "Login e registro de usuários administrativos")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Realizar login e obter token JWT")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/registro")
    @Operation(summary = "Registrar novo usuário administrativo")
    public ResponseEntity<Void> registro(@RequestBody LoginRequest request) {
        authService.registro(request);
        return ResponseEntity.ok().build();
    }
}
