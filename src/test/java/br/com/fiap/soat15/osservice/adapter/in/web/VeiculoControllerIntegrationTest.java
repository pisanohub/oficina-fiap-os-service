package br.com.fiap.soat15.osservice.adapter.in.web;

import br.com.fiap.soat15.osservice.application.dto.VeiculoDto;
import br.com.fiap.soat15.osservice.config.security.JwtAuthFilter;
import br.com.fiap.soat15.osservice.domain.service.VeiculoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VeiculoController.class)
@AutoConfigureMockMvc(addFilters = false)
class VeiculoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private VeiculoService veiculoService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    private static final Long CLIENTE_ID = 1L;

    private VeiculoDto veiculoDTO(Long id, String placa) {
        return VeiculoDto.builder()
                .id(id)
                .placa(placa)
                .marca("Toyota")
                .modelo("Corolla")
                .ano(2022)
                .clienteId(CLIENTE_ID)
                .build();
    }

    private Map<String, Object> requestBody(
            String placa, Long clienteId) {

        return Map.of(
                "placa", placa,
                "marca", "Toyota",
                "modelo", "Corolla",
                "ano", 2022,
                "clienteId", clienteId
        );
    }

    @Test
    @DisplayName("GET /api/v1/veiculos - deve retornar lista vazia")
    void deveRetornarListaVazia() throws Exception {

        when(veiculoService.listarVeiculos())
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/veiculos"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/veiculos - deve listar veículos cadastrados")
    void deveListarVeiculos() throws Exception {

        when(veiculoService.listarVeiculos())
                .thenReturn(List.of(
                        veiculoDTO(1L, "ABC1D23"),
                        veiculoDTO(2L, "XYZ9W87")
                ));

        mockMvc.perform(get("/api/v1/veiculos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/v1/veiculos/{id} - deve retornar veículo por ID")
    void deveBuscarVeiculoPorId() throws Exception {

        when(veiculoService.buscarPorId(1L))
                .thenReturn(veiculoDTO(1L, "ABC1D23"));

        mockMvc.perform(get("/api/v1/veiculos/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placa", is("ABC1D23")))
                .andExpect(jsonPath("$.marca", is("Toyota")))
                .andExpect(jsonPath("$.modelo", is("Corolla")))
                .andExpect(jsonPath("$.clienteId", is(1)));
    }

    @Test
    @DisplayName("GET /api/v1/veiculos/{id} - deve retornar 404 quando não encontrado")
    void deveRetornarErroQuandoVeiculoNaoEncontrado() throws Exception {

        when(veiculoService.buscarPorId(999L))
                .thenThrow(new NoSuchElementException("Veículo não encontrado"));

        mockMvc.perform(get("/api/v1/veiculos/{id}", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/veiculos - deve criar veículo com sucesso")
    void deveCriarVeiculo() throws Exception {

        when(veiculoService.criar(any(VeiculoDto.class)))
                .thenReturn(veiculoDTO(1L, "ABC1D23"));

        mockMvc.perform(post("/api/v1/veiculos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                requestBody("ABC1D23", CLIENTE_ID))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.placa", is("ABC1D23")))
                .andExpect(jsonPath("$.clienteId", is(1)));
    }

    @Test
    @DisplayName("POST /api/v1/veiculos - deve aceitar placa no formato antigo")
    void deveAceitarPlacaFormatoAntigo() throws Exception {

        when(veiculoService.criar(any()))
                .thenReturn(veiculoDTO(1L, "ABC-1234"));

        mockMvc.perform(post("/api/v1/veiculos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                requestBody("ABC-1234", CLIENTE_ID))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.placa", is("ABC-1234")));
    }

    @Test
    @DisplayName("POST /api/v1/veiculos - deve retornar 400 com placa inválida")
    void deveRetornarErroComPlacaInvalida() throws Exception {

        mockMvc.perform(post("/api/v1/veiculos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                requestBody("INVALIDA", CLIENTE_ID))))
                .andExpect(status().is4xxClientError());

        verifyNoInteractions(veiculoService);
    }

    @Test
    @DisplayName("POST /api/v1/veiculos - deve retornar erro com placa vazia")
    void deveRetornarErroComPlacaVazia() throws Exception {

        mockMvc.perform(post("/api/v1/veiculos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                requestBody("", CLIENTE_ID))))
                .andExpect(status().is4xxClientError());

        verifyNoInteractions(veiculoService);
    }

    @Test
    @DisplayName("POST /api/v1/veiculos - deve retornar 400 com placa duplicada")
    void deveRetornarErroComPlacaDuplicada() throws Exception {

        when(veiculoService.criar(any()))
                .thenThrow(new IllegalArgumentException("Placa já cadastrada"));

        mockMvc.perform(post("/api/v1/veiculos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                requestBody("ABC1D23", CLIENTE_ID))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/veiculos - deve retornar 404 com cliente inexistente")
    void deveRetornarErroComClienteInexistente() throws Exception {

        when(veiculoService.criar(any()))
                .thenThrow(new IllegalArgumentException("Cliente não encontrado"));

        mockMvc.perform(post("/api/v1/veiculos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                requestBody("ABC1D23", 9999L))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/veiculos/{id} - deve atualizar veículo com sucesso")
    void deveAtualizarVeiculo() throws Exception {

        when(veiculoService.atualizar(eq(1L), any()))
                .thenReturn(veiculoDTO(1L, "XYZ9W87"));

        mockMvc.perform(put("/api/v1/veiculos/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                requestBody("XYZ9W87", CLIENTE_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marca", is("Toyota")))
                .andExpect(jsonPath("$.placa", is("XYZ9W87")))
                .andExpect(jsonPath("$.clienteId", is(1)));
    }

    @Test
    @DisplayName("DELETE /api/v1/veiculos/{id} - deve deletar veículo com sucesso")
    void deveDeletarVeiculo() throws Exception {

        doNothing().when(veiculoService).deletar(1L);

        mockMvc.perform(delete("/api/v1/veiculos/{id}", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/veiculos/{id} - deve retornar 404 ao deletar inexistente")
    void deveRetornarErroAoDeletarInexistente() throws Exception {

        doThrow(new NoSuchElementException("Veículo não encontrado"))
                .when(veiculoService).deletar(999L);

        mockMvc.perform(delete("/api/v1/veiculos/{id}", 999L))
                .andExpect(status().isNotFound());
    }
}
