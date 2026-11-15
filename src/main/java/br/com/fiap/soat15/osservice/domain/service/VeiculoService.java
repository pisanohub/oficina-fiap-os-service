package br.com.fiap.soat15.osservice.domain.service;

import br.com.fiap.soat15.osservice.application.dto.VeiculoDto;

import java.util.List;

public interface VeiculoService {

    List<VeiculoDto> listarVeiculos();

    VeiculoDto buscarPorId(Long id);

    VeiculoDto criar(VeiculoDto dto);

    VeiculoDto atualizar(Long id, VeiculoDto dto);

    void deletar(Long id);
}
