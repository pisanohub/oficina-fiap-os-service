package br.com.fiap.soat15.osservice.adapter.out.gateway;

import br.com.fiap.soat15.osservice.adapter.out.gateway.dto.ItemEstoqueResponseDTO;
import br.com.fiap.soat15.osservice.adapter.out.gateway.dto.ServicoResponseDTO;
import br.com.fiap.soat15.osservice.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "execucao-service", configuration = FeignClientConfig.class)
public interface ExecucaoClient {

    @GetMapping("/api/v1/servicos/{id}")
    ServicoResponseDTO buscarServicoPorId(@PathVariable("id") Long id);

    @GetMapping("/api/v1/item-estoque/{id}")
    ItemEstoqueResponseDTO buscarItemPorId(@PathVariable("id") Long id);
}
