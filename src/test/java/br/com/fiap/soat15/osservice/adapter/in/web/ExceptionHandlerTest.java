package br.com.fiap.soat15.osservice.adapter.in.web;

import br.com.fiap.soat15.osservice.domain.exception.BusinessException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import java.util.NoSuchElementException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private WebRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/test");
    }

    @Test
    @DisplayName("Deve retornar 404 para NoSuchElementException")
    void deveRetornar404ParaNoSuchElementException() {
        ResponseEntity<ErrorResponse> response =
                handler.handleNoSuchElementException(new NoSuchElementException("Recurso não encontrado"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).isEqualTo("Recurso não encontrado");
        assertThat(response.getBody().getPath()).isEqualTo("/test");
    }

    @Test
    @DisplayName("Deve retornar 400 para IllegalArgumentException")
    void deveRetornar400ParaIllegalArgumentException() {
        ResponseEntity<ErrorResponse> response =
                handler.handleIllegalArgumentException(new IllegalArgumentException("Argumento inválido"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("Argumento inválido");
        assertThat(response.getBody().getPath()).isEqualTo("/test");
    }

    @Test
    @DisplayName("Deve retornar 400 para ConstraintViolationException")
    void deveRetornar400ParaConstraintViolationException() {
        ConstraintViolationException ex = new ConstraintViolationException("Violação de constraint", Set.of());

        ResponseEntity<ErrorResponse> response =
                handler.handleConstraintViolationException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getPath()).isEqualTo("/test");
    }

    @Test
    @DisplayName("Deve retornar 400 para BusinessException")
    void deveRetornar400ParaBusinessException() {
        ResponseEntity<ErrorResponse> response =
                handler.handleBusinessException(new BusinessException("Erro de negócio"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("Erro de negócio");
    }

    @Test
    @DisplayName("Deve retornar 500 para Exception genérica")
    void deveRetornar500ParaExcecaoGenerica() {
        ResponseEntity<ErrorResponse> response =
                handler.handleGlobalException(new RuntimeException("Erro inesperado"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }
}
