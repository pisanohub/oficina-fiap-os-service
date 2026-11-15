package br.com.fiap.soat15.osservice.adapter.in.web;

import br.com.fiap.soat15.osservice.application.dto.ClienteDTO;
import br.com.fiap.soat15.osservice.domain.entity.Cliente;
import br.com.fiap.soat15.osservice.domain.service.ClienteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class ClienteControllerTest {

    @Mock
    private ClienteService clienteService;

    @InjectMocks
    private ClienteController clienteController;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(clienteController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void criarCliente_DeveRetornarClienteCriado() throws Exception {
        ClienteDTO dto = ClienteDTO.builder()
                .nome("João Silva")
                .cpfCnpj("12345678901")
                .email("joao@example.com")
                .telefone("999999999")
                .endereco("Rua A, 123")
                .build();

        Cliente clienteCriado = Cliente.builder()
                .id(1L)
                .nome("João Silva")
                .cpfCnpj("12345678901")
                .email("joao@example.com")
                .telefone("999999999")
                .endereco("Rua A, 123")
                .ativo(true)
                .build();

        when(clienteService.criarCliente(any(Cliente.class))).thenReturn(clienteCriado);

        mockMvc.perform(post("/api/v1/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nome").value("João Silva"))
                .andExpect(jsonPath("$.cpfCnpj").value("12345678901"))
                .andExpect(jsonPath("$.email").value("joao@example.com"))
                .andExpect(jsonPath("$.telefone").value("999999999"))
                .andExpect(jsonPath("$.endereco").value("Rua A, 123"))
                .andExpect(jsonPath("$.ativo").value(true));

        verify(clienteService, times(1)).criarCliente(any(Cliente.class));
    }

    @Test
    void criarCliente_ComValidacaoInvalida_DeveRetornarBadRequest() throws Exception {
        ClienteDTO dto = ClienteDTO.builder()
                .nome("") // Nome vazio - inválido
                .cpfCnpj("123") // CPF/CNPJ inválido
                .email("email-invalido") // Email inválido
                .telefone("123") // Telefone inválido
                .build();

        mockMvc.perform(post("/api/v1/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Erro de validação nos campos fornecidos"))
                .andExpect(jsonPath("$.validationErrors").exists());

        verify(clienteService, never()).criarCliente(any());
    }

    @Test
    void obterClientePorId_DeveRetornarCliente() throws Exception {
        Cliente cliente = Cliente.builder()
                .id(1L)
                .nome("João Silva")
                .cpfCnpj("12345678901")
                .email("joao@example.com")
                .telefone("999999999")
                .endereco("Rua A, 123")
                .ativo(true)
                .build();

        when(clienteService.obterClientePorId(1L)).thenReturn(cliente);

        mockMvc.perform(get("/api/v1/cliente/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nome").value("João Silva"));

        verify(clienteService, times(1)).obterClientePorId(1L);
    }

    @Test
    void obterClientePorId_ComIdInexistente_DeveRetornarNotFound() throws Exception {
        when(clienteService.obterClientePorId(999L)).thenThrow(new NoSuchElementException("Cliente não encontrado"));

        mockMvc.perform(get("/api/v1/cliente/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Cliente não encontrado"));

        verify(clienteService, times(1)).obterClientePorId(999L);
    }

    @Test
    void obterClientePorCpfCnpj_DeveRetornarCliente() throws Exception {
        Cliente cliente = Cliente.builder()
                .id(1L)
                .nome("João Silva")
                .cpfCnpj("12345678901")
                .email("joao@example.com")
                .telefone("999999999")
                .endereco("Rua A, 123")
                .ativo(true)
                .build();

        when(clienteService.obterClientePorCpfCnpj("12345678901")).thenReturn(cliente);

        mockMvc.perform(get("/api/v1/cliente/cpfcnpj/12345678901"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpfCnpj").value("12345678901"));

        verify(clienteService, times(1)).obterClientePorCpfCnpj("12345678901");
    }

    @Test
    void obterClientePorCpfCnpj_ComCpfInexistente_DeveRetornarNotFound() throws Exception {
        when(clienteService.obterClientePorCpfCnpj("99999999999")).thenThrow(new NoSuchElementException("Cliente não encontrado"));

        mockMvc.perform(get("/api/v1/cliente/cpfcnpj/99999999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Cliente não encontrado"));

        verify(clienteService, times(1)).obterClientePorCpfCnpj("99999999999");
    }

    @Test
    void listarClientes_DeveRetornarLista() throws Exception {
        Cliente cliente1 = Cliente.builder()
                .id(1L)
                .nome("João Silva")
                .cpfCnpj("12345678901")
                .email("joao@example.com")
                .telefone("999999999")
                .endereco("Rua A, 123")
                .ativo(true)
                .build();

        Cliente cliente2 = Cliente.builder()
                .id(2L)
                .nome("Maria Oliveira")
                .cpfCnpj("98765432100")
                .email("maria@example.com")
                .telefone("888888888")
                .endereco("Rua B, 456")
                .ativo(true)
                .build();

        when(clienteService.listarClientes()).thenReturn(List.of(cliente1, cliente2));

        mockMvc.perform(get("/api/v1/cliente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].nome").value("João Silva"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].nome").value("Maria Oliveira"));

        verify(clienteService, times(1)).listarClientes();
    }

    @Test
    void atualizarCliente_DeveRetornarClienteAtualizado() throws Exception {
        ClienteDTO dto = ClienteDTO.builder()
                .nome("João Silva Atualizado")
                .cpfCnpj("12345678901")
                .email("joao.atualizado@example.com")
                .telefone("999999999")
                .endereco("Rua A, 123 Atualizado")
                .build();

        Cliente clienteAtualizado = Cliente.builder()
                .id(1L)
                .nome("João Silva Atualizado")
                .cpfCnpj("12345678901")
                .email("joao.atualizado@example.com")
                .telefone("999999999")
                .endereco("Rua A, 123 Atualizado")
                .ativo(true)
                .build();

        when(clienteService.atualizarCliente(eq(1L), any(Cliente.class))).thenReturn(clienteAtualizado);

        mockMvc.perform(put("/api/v1/cliente/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nome").value("João Silva Atualizado"))
                .andExpect(jsonPath("$.email").value("joao.atualizado@example.com"));

        verify(clienteService, times(1)).atualizarCliente(eq(1L), any(Cliente.class));
    }

    @Test
    void atualizarCliente_ComIdInexistente_DeveRetornarNotFound() throws Exception {
        ClienteDTO dto = ClienteDTO.builder()
                .nome("João Silva")
                .cpfCnpj("12345678901")
                .email("joao@example.com")
                .telefone("999999999")
                .endereco("Rua A, 123")
                .build();

        when(clienteService.atualizarCliente(eq(999L), any(Cliente.class)))
                .thenThrow(new NoSuchElementException("Cliente não encontrado"));

        mockMvc.perform(put("/api/v1/cliente/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Cliente não encontrado"));
    }

    @Test
    void deletarCliente_DeveRetornarNoContent() throws Exception {
        doNothing().when(clienteService).deletarCliente(1L);

        mockMvc.perform(delete("/api/v1/cliente/1"))
                .andExpect(status().isNoContent());

        verify(clienteService, times(1)).deletarCliente(1L);
    }

    @Test
    void deletarCliente_ComIdInexistente_DeveRetornarNotFound() throws Exception {
        doThrow(new NoSuchElementException("Cliente não encontrado")).when(clienteService).deletarCliente(999L);

        mockMvc.perform(delete("/api/v1/cliente/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Cliente não encontrado"));
    }
}