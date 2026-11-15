package br.com.fiap.soat15.osservice.application.usecase;

import br.com.fiap.soat15.osservice.adapter.out.persistence.repository.ClienteRepository;
import br.com.fiap.soat15.osservice.domain.entity.Cliente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceImplTest {

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClienteServiceImpl clienteService;

    private Long id;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        id = 1L;
        cliente = Cliente.builder()
                .id(id)
                .nome("João Silva")
                .cpfCnpj("64818919063")
                .email("joao@example.com")
                .telefone("999999999")
                .endereco("Rua A, 123")
                .ativo(true)
                .build();
    }

    @Test
    @DisplayName("Deve listar todos os clientes")
    void deveListarTodosClientes() {
        when(clienteRepository.findAll()).thenReturn(List.of(cliente));

        List<Cliente> resultado = clienteService.listarClientes();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.getFirst().getCpfCnpj()).isEqualTo("64818919063");
        verify(clienteRepository).findAll();
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não há clientes")
    void deveRetornarListaVazia() {
        when(clienteRepository.findAll()).thenReturn(List.of());

        List<Cliente> resultado = clienteService.listarClientes();

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Deve buscar cliente por ID com sucesso")
    void deveBuscarClientePorId() {
        when(clienteRepository.findById(id)).thenReturn(Optional.of(cliente));

        Cliente resultado = clienteService.obterClientePorId(id);

        assertThat(resultado.getId()).isEqualTo(id);
        assertThat(resultado.getCpfCnpj()).isEqualTo("64818919063");
    }

    @Test
    @DisplayName("Deve lançar exceção quando cliente não encontrado por ID")
    void deveLancarExcecaoQuandoClienteNaoEncontrado() {
        when(clienteRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.obterClientePorId(id))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("não encontrado");
    }

    @Test
    @DisplayName("Deve criar cliente com sucesso")
    void deveCriarClienteComSucesso() {
        when(clienteRepository.findByCpfCnpj(cliente.getCpfCnpj())).thenReturn(Optional.empty());
        when(clienteRepository.save(any(Cliente.class))).thenReturn(cliente);

        Cliente resultado = clienteService.criarCliente(cliente);

        assertThat(resultado.getCpfCnpj()).isEqualTo("64818919063");
        verify(clienteRepository).save(any(Cliente.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar cliente com CPF/CNPJ duplicado")
    void deveLancarExcecaoComCpfCnpjDuplicado() {
        when(clienteRepository.findByCpfCnpj(cliente.getCpfCnpj())).thenReturn(Optional.of(cliente));

        assertThatThrownBy(() -> clienteService.criarCliente(cliente))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CPF/CNPJ já cadastrado");

        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve atualizar cliente com sucesso")
    void deveAtualizarClienteComSucesso() {
        Cliente clienteAtualizado = Cliente.builder()
                .id(id)
                .nome("Jurandir Silva")
                .cpfCnpj("22139483057")
                .email("jurandir@example.com")
                .telefone("188889999")
                .endereco("Rua B12, 321")
                .ativo(true)
                .build();

        when(clienteRepository.findById(id)).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any(Cliente.class))).thenReturn(clienteAtualizado);

        Cliente resultado = clienteService.atualizarCliente(id, clienteAtualizado);

        assertThat(resultado.getNome()).isEqualTo("Jurandir Silva");
    }

    @Test
    @DisplayName("Deve lançar exceção ao atualizar cliente inexistente")
    void deveLancarExcecaoAoAtualizarClienteInexistente() {
        when(clienteRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.atualizarCliente(id, cliente))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("não encontrado");
    }

    @Test
    @DisplayName("Deve deletar cliente com sucesso (soft delete)")
    void deveDeletarClienteComSucesso() {
        when(clienteRepository.findById(id)).thenReturn(Optional.of(cliente));

        clienteService.deletarCliente(id);

        verify(clienteRepository).save(any(Cliente.class));
        assertThat(cliente.getAtivo()).isFalse();
    }

    @Test
    @DisplayName("Deve lançar exceção ao deletar cliente inexistente")
    void deveLancarExcecaoAoDeletarClienteInexistente() {
        when(clienteRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.deletarCliente(id))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("não encontrado");

        verify(clienteRepository, never()).save(any());
    }
}
