package br.com.fiap.soat15.osservice.application.usecase;

import br.com.fiap.soat15.osservice.adapter.out.persistence.repository.ClienteRepository;
import br.com.fiap.soat15.osservice.adapter.out.persistence.repository.VeiculoRepository;
import br.com.fiap.soat15.osservice.application.dto.VeiculoDto;
import br.com.fiap.soat15.osservice.domain.entity.Cliente;
import br.com.fiap.soat15.osservice.domain.entity.Veiculo;
import br.com.fiap.soat15.osservice.domain.service.VeiculoService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@AllArgsConstructor
public class VeiculoServiceImpl implements VeiculoService {

    private final VeiculoRepository veiculoRepository;
    private final ClienteRepository clienteRepository;

    @Override
    public List<VeiculoDto> listarVeiculos() {
        return veiculoRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public VeiculoDto buscarPorId(Long id) {
        return veiculoRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new NoSuchElementException("Veículo não encontrado: " + id));
    }

    @Override
    public VeiculoDto criar(VeiculoDto dto) {
        if (veiculoRepository.existsByPlaca(dto.getPlaca())) {
            throw new IllegalArgumentException("Já existe um veículo com a placa: " + dto.getPlaca());
        }
        Cliente cliente = buscarCliente(dto.getClienteId());
        Veiculo entity = toEntity(dto, cliente);
        return toDto(veiculoRepository.save(entity));
    }

    @Override
    public VeiculoDto atualizar(Long id, VeiculoDto dto) {
        Veiculo entity = veiculoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Veículo não encontrado: " + id));
        entity.setPlaca(dto.getPlaca());
        entity.setMarca(dto.getMarca());
        entity.setModelo(dto.getModelo());
        entity.setAno(dto.getAno());
        entity.setCliente(buscarCliente(dto.getClienteId()));
        return toDto(veiculoRepository.save(entity));
    }

    @Override
    public void deletar(Long id) {
        if (!veiculoRepository.existsById(id)) {
            throw new NoSuchElementException("Veículo não encontrado: " + id);
        }
        veiculoRepository.deleteById(id);
    }

    private VeiculoDto toDto(Veiculo entity) {
        return VeiculoDto.builder()
                .id(entity.getId())
                .placa(entity.getPlaca())
                .marca(entity.getMarca())
                .modelo(entity.getModelo())
                .ano(entity.getAno())
                .clienteId(entity.getCliente().getId())
                .build();
    }

    private Cliente buscarCliente(Long clienteId) {
        if (clienteId == null) {
            throw new IllegalArgumentException("clienteId é obrigatório");
        }
        return clienteRepository.findById(clienteId)
                .orElseThrow(() -> new NoSuchElementException("Cliente não encontrado: " + clienteId));
    }

    private Veiculo toEntity(VeiculoDto dto, Cliente cliente) {
        return Veiculo.builder()
                .placa(dto.getPlaca())
                .marca(dto.getMarca())
                .modelo(dto.getModelo())
                .ano(dto.getAno())
                .cliente(cliente)
                .build();
    }
}
