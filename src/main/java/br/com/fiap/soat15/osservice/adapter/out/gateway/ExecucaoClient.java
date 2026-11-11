package br.com.fiap.soat15.osservice.adapter.out.gateway;

import br.com.fiap.soat15.osservice.adapter.out.gateway.dto.CatalogoResponseDTO;
import br.com.fiap.soat15.osservice.config.FeignClientConfig;
import br.com.fiap.soat15.osservice.domain.entity.TipoItem;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "execucao-service", configuration = FeignClientConfig.class)
public interface ExecucaoClient {

    @GetMapping("/api/v1/catalogo/{tipo}/{itemId}")
    CatalogoResponseDTO getItem(@PathVariable("tipo") TipoItem tipoItem, @PathVariable("itemId") String itemId);
}
