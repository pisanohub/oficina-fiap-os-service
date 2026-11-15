package br.com.fiap.soat15.osservice.domain.service;

import br.com.fiap.soat15.osservice.domain.entity.Cliente;

import java.util.List;

public interface ClienteService {

    Cliente criarCliente(Cliente cliente);

    Cliente atualizarCliente(Long id, Cliente clienteAtualizado);

    void deletarCliente(Long id);

    Cliente obterClientePorId(Long id);

    Cliente obterClientePorCpfCnpj(String cpfCnpj);

    List<Cliente> listarClientes();
}
