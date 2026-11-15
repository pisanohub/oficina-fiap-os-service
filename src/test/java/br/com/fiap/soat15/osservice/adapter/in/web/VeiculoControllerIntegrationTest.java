package br.com.fiap.soat15.osservice.adapter.in.web;

import br.com.fiap.soat15.osservice.adapter.out.persistence.repository.ClienteRepository;
import br.com.fiap.soat15.osservice.adapter.out.persistence.repository.VeiculoRepository;
import br.com.fiap.soat15.osservice.domain.entity.Cliente;
import br.com.fiap.soat15.osservice.domain.entity.Veiculo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Sql(
        scripts = "/cleanup.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class VeiculoControllerIntegrationTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private VeiculoRepository veiculoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    private Cliente clienteSalvo;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        clienteSalvo = clienteRepository.save(Cliente.builder()
                .nome("Cliente Teste")
                .cpfCnpj("11144477735")
                .email("teste@email.com")
                .ativo(true)
                .build());
    }

    @Test
    @DisplayName("GET /api/v1/veiculos - deve retornar lista vazia")
    void deveRetornarListaVazia() throws Exception {
        mockMvc.perform(get("/api/v1/veiculos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/v1/veiculos - deve listar veículos cadastrados")
    void deveListarVeiculos() throws Exception {
        veiculoRepository.save(veiculoEntity("ABC1D23"));
        veiculoRepository.save(veiculoEntity("XYZ9W87"));

        mockMvc.perform(get("/api/v1/veiculos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/v1/veiculos/{id} - deve retornar veículo por ID")
    void deveBuscarVeiculoPorId() throws Exception {
        Veiculo saved = veiculoRepository.save(veiculoEntity("ABC1D23"));

        mockMvc.perform(get("/api/v1/veiculos/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placa", is("ABC1D23")))
                .andExpect(jsonPath("$.marca", is("Toyota")))
                .andExpect(jsonPath("$.modelo", is("Corolla")))
                .andExpect(jsonPath("$.clienteId", is(clienteSalvo.getId().intValue())));
    }

    @Test
    @DisplayName("GET /api/v1/veiculos/{id} - deve retornar 404 quando não encontrado")
    void deveRetornarErroQuandoVeiculoNaoEncontrado() throws Exception {
        mockMvc.perform(get("/api/v1/veiculos/{id}", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/veiculos - deve criar veículo com sucesso")
    void deveCriarVeiculo() throws Exception {
        Map<String, Object> body = Map.of(
                "placa", "ABC1D23",
                "marca", "Toyota",
                "modelo", "Corolla",
                "ano", 2022,
                "clienteId", clienteSalvo.getId()
        );

        mockMvc.perform(post("/api/v1/veiculos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.placa", is("ABC1D23")))
                .andExpect(jsonPath("$.clienteId", is(clienteSalvo.getId().intValue())));
    }

    @Test
    @DisplayName("POST /api/v1/veiculos - deve aceitar placa no formato antigo")
    void deveAceitarPlacaFormatoAntigo() throws Exception {
        Map<String, Object> body = Map.of(
                "placa", "ABC-1234",
                "marca", "Toyota",
                "modelo", "Corolla",
                "ano", 2022,
                "clienteId", clienteSalvo.getId()
        );

        mockMvc.perform(post("/api/v1/veiculos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.placa", is("ABC-1234")));
    }

    @Test
    @DisplayName("POST /api/v1/veiculos - deve retornar 400 com placa inválida")
    void deveRetornarErroComPlacaInvalida() throws Exception {
        Map<String, Object> body = Map.of(
                "placa", "INVALIDA",
                "marca", "Toyota",
                "modelo", "Corolla",
                "ano", 2022,
                "clienteId", clienteSalvo.getId()
        );

        mockMvc.perform(post("/api/v1/veiculos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("POST /api/v1/veiculos - deve retornar erro com placa vazia")
    void deveRetornarErroComPlacaVazia() throws Exception {
        Map<String, Object> body = Map.of(
                "placa", "",
                "marca", "Toyota",
                "modelo", "Corolla",
                "ano", 2022,
                "clienteId", clienteSalvo.getId()
        );

        mockMvc.perform(post("/api/v1/veiculos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("POST /api/v1/veiculos - deve retornar 400 com placa duplicada")
    void deveRetornarErroComPlacaDuplicada() throws Exception {
        veiculoRepository.save(veiculoEntity("ABC1D23"));

        Map<String, Object> body = Map.of(
                "placa", "ABC1D23",
                "marca", "Honda",
                "modelo", "Civic",
                "ano", 2023,
                "clienteId", clienteSalvo.getId()
        );

        mockMvc.perform(post("/api/v1/veiculos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/veiculos - deve retornar 404 com cliente inexistente")
    void deveRetornarErroComClienteInexistente() throws Exception {
        Map<String, Object> body = Map.of(
                "placa", "ABC1D23",
                "marca", "Toyota",
                "modelo", "Corolla",
                "ano", 2022,
                "clienteId", 9999L
        );

        mockMvc.perform(post("/api/v1/veiculos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/veiculos/{id} - deve atualizar veículo com sucesso")
    void deveAtualizarVeiculo() throws Exception {
        Veiculo saved = veiculoRepository.save(veiculoEntity("ABC1D23"));

        Map<String, Object> body = Map.of(
                "placa", "XYZ9W87",
                "marca", "Honda",
                "modelo", "Civic",
                "ano", 2023,
                "clienteId", clienteSalvo.getId()
        );

        mockMvc.perform(put("/api/v1/veiculos/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marca", is("Honda")))
                .andExpect(jsonPath("$.placa", is("XYZ9W87")))
                .andExpect(jsonPath("$.clienteId", is(clienteSalvo.getId().intValue())));
    }

    @Test
    @DisplayName("DELETE /api/v1/veiculos/{id} - deve deletar veículo com sucesso")
    void deveDeletarVeiculo() throws Exception {
        Veiculo saved = veiculoRepository.save(veiculoEntity("ABC1D23"));

        mockMvc.perform(delete("/api/v1/veiculos/{id}", saved.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/veiculos/{id} - deve retornar 404 ao deletar inexistente")
    void deveRetornarErroAoDeletarInexistente() throws Exception {
        mockMvc.perform(delete("/api/v1/veiculos/{id}", 999L))
                .andExpect(status().isNotFound());
    }

    private Veiculo veiculoEntity(String placa) {
        return Veiculo.builder()
                .placa(placa)
                .marca("Toyota")
                .modelo("Corolla")
                .ano(2022)
                .cliente(clienteSalvo)
                .build();
    }
}
