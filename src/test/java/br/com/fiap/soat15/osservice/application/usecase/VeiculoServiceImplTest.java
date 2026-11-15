package br.com.fiap.soat15.osservice.application.usecase;

import br.com.fiap.soat15.osservice.adapter.out.persistence.repository.ClienteRepository;
import br.com.fiap.soat15.osservice.adapter.out.persistence.repository.VeiculoRepository;
import br.com.fiap.soat15.osservice.application.dto.VeiculoDto;
import br.com.fiap.soat15.osservice.domain.entity.Cliente;
import br.com.fiap.soat15.osservice.domain.entity.Veiculo;
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
class VeiculoServiceImplTest {

    @Mock
    private VeiculoRepository veiculoRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private VeiculoServiceImpl veiculoService;

    private Long id;
    private Long clienteId;
    private Cliente cliente;
    private Veiculo entity;
    private VeiculoDto dto;

    @BeforeEach
    void setUp() {
        id = 1L;
        clienteId = 2L;

        cliente = Cliente.builder()
                .id(clienteId)
                .nome("João da Silva")
                .cpfCnpj("11144477735")
                .ativo(true)
                .build();

        entity = Veiculo.builder()
                .id(id)
                .placa("ABC1D23")
                .marca("Toyota")
                .modelo("Corolla")
                .ano(2022)
                .cliente(cliente)
                .build();

        dto = VeiculoDto.builder()
                .placa("ABC1D23")
                .marca("Toyota")
                .modelo("Corolla")
                .ano(2022)
                .clienteId(clienteId)
                .build();
    }

    @Test
    @DisplayName("Deve listar todos os veículos")
    void deveListarTodosVeiculos() {
        when(veiculoRepository.findAll()).thenReturn(List.of(entity));

        List<VeiculoDto> resultado = veiculoService.listarVeiculos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getPlaca()).isEqualTo("ABC1D23");
        assertThat(resultado.get(0).getClienteId()).isEqualTo(clienteId);
        verify(veiculoRepository).findAll();
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não há veículos")
    void deveRetornarListaVaziaQuandoNaoHaVeiculos() {
        when(veiculoRepository.findAll()).thenReturn(List.of());

        List<VeiculoDto> resultado = veiculoService.listarVeiculos();

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Deve buscar veículo por ID com sucesso")
    void deveBuscarVeiculoPorId() {
        when(veiculoRepository.findById(id)).thenReturn(Optional.of(entity));

        VeiculoDto resultado = veiculoService.buscarPorId(id);

        assertThat(resultado.getId()).isEqualTo(id);
        assertThat(resultado.getPlaca()).isEqualTo("ABC1D23");
        assertThat(resultado.getClienteId()).isEqualTo(clienteId);
    }

    @Test
    @DisplayName("Deve lançar exceção quando veículo não encontrado por ID")
    void deveLancarExcecaoQuandoVeiculoNaoEncontrado() {
        when(veiculoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> veiculoService.buscarPorId(id))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    @DisplayName("Deve criar veículo com sucesso")
    void deveCriarVeiculoComSucesso() {
        when(veiculoRepository.existsByPlaca(dto.getPlaca())).thenReturn(false);
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
        when(veiculoRepository.save(any(Veiculo.class))).thenReturn(entity);

        VeiculoDto resultado = veiculoService.criar(dto);

        assertThat(resultado.getPlaca()).isEqualTo("ABC1D23");
        assertThat(resultado.getClienteId()).isEqualTo(clienteId);
        verify(clienteRepository).findById(clienteId);
        verify(veiculoRepository).save(any(Veiculo.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar veículo com placa duplicada")
    void deveLancarExcecaoComPlacaDuplicada() {
        when(veiculoRepository.existsByPlaca(dto.getPlaca())).thenReturn(true);

        assertThatThrownBy(() -> veiculoService.criar(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ABC1D23");

        verify(veiculoRepository, never()).save(any());
        verify(clienteRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar veículo com cliente inexistente")
    void deveLancarExcecaoAoCriarVeiculoComClienteInexistente() {
        when(veiculoRepository.existsByPlaca(dto.getPlaca())).thenReturn(false);
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> veiculoService.criar(dto))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining(clienteId.toString());

        verify(veiculoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar veículo sem clienteId")
    void deveLancarExcecaoAoCriarVeiculoSemClienteId() {
        VeiculoDto dtoSemCliente = VeiculoDto.builder()
                .placa("ABC1D23")
                .marca("Toyota")
                .modelo("Corolla")
                .ano(2022)
                .build();
        when(veiculoRepository.existsByPlaca(dtoSemCliente.getPlaca())).thenReturn(false);

        assertThatThrownBy(() -> veiculoService.criar(dtoSemCliente))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("clienteId é obrigatório");

        verify(veiculoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve atualizar veículo com sucesso")
    void deveAtualizarVeiculoComSucesso() {
        VeiculoDto dtoAtualizado = VeiculoDto.builder()
                .placa("XYZ9W87")
                .marca("Honda")
                .modelo("Civic")
                .ano(2023)
                .clienteId(clienteId)
                .build();

        Veiculo entityAtualizada = Veiculo.builder()
                .id(id).placa("XYZ9W87").marca("Honda").modelo("Civic").ano(2023).cliente(cliente).build();

        when(veiculoRepository.findById(id)).thenReturn(Optional.of(entity));
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
        when(veiculoRepository.save(any(Veiculo.class))).thenReturn(entityAtualizada);

        VeiculoDto resultado = veiculoService.atualizar(id, dtoAtualizado);

        assertThat(resultado.getMarca()).isEqualTo("Honda");
        assertThat(resultado.getModelo()).isEqualTo("Civic");
        assertThat(resultado.getClienteId()).isEqualTo(clienteId);
        verify(clienteRepository).findById(clienteId);
    }

    @Test
    @DisplayName("Deve lançar exceção ao atualizar veículo inexistente")
    void deveLancarExcecaoAoAtualizarVeiculoInexistente() {
        when(veiculoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> veiculoService.atualizar(id, dto))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("Deve lançar exceção ao atualizar veículo com cliente inexistente")
    void deveLancarExcecaoAoAtualizarComClienteInexistente() {
        when(veiculoRepository.findById(id)).thenReturn(Optional.of(entity));
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> veiculoService.atualizar(id, dto))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining(clienteId.toString());
    }

    @Test
    @DisplayName("Deve deletar veículo com sucesso")
    void deveDeletarVeiculoComSucesso() {
        when(veiculoRepository.existsById(id)).thenReturn(true);

        veiculoService.deletar(id);

        verify(veiculoRepository).deleteById(id);
    }

    @Test
    @DisplayName("Deve lançar exceção ao deletar veículo inexistente")
    void deveLancarExcecaoAoDeletarVeiculoInexistente() {
        when(veiculoRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> veiculoService.deletar(id))
                .isInstanceOf(NoSuchElementException.class);

        verify(veiculoRepository, never()).deleteById(any());
    }
}
