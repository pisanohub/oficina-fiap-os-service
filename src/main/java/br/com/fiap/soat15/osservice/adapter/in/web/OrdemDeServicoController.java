package br.com.fiap.soat15.osservice.adapter.in.web;

import br.com.fiap.soat15.osservice.adapter.out.persistence.repository.SagaHistoricoRepository;
import br.com.fiap.soat15.osservice.application.dto.*;
import br.com.fiap.soat15.osservice.domain.entity.SagaHistorico;
import br.com.fiap.soat15.osservice.domain.entity.StatusOrcamento;
import br.com.fiap.soat15.osservice.domain.service.OrdemDeServicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ordens")
@RequiredArgsConstructor
@Tag(name = "Ordens de Serviço", description = "Gestão do ciclo de vida das ordens de serviço")
@Validated
public class OrdemDeServicoController {

    private final OrdemDeServicoService ordemService;
    private final SagaHistoricoRepository historicoRepository;

    @PostMapping
    @Operation(summary = "Abrir nova ordem de serviço")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "OS criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou veículo não pertence ao cliente"),
            @ApiResponse(responseCode = "404", description = "Cliente ou veículo não encontrado")
    })
    public ResponseEntity<OrdemDeServicoDTO> criarOrdem(@Valid @RequestBody CriarOrdemDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ordemService.criarOrdem(dto));
    }

    @PostMapping("/{id}/orcamento")
    @Operation(summary = "Solicitar orçamento para uma ordem de serviço")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orçamento solicitado"),
            @ApiResponse(responseCode = "404", description = "OS não encontrada")
    })
    public ResponseEntity<OrdemDeServicoDTO> solicitarOrcamento(@PathVariable Long id){
        return ResponseEntity.ok(ordemService.solicitarOrcamento(id));
    }

    @GetMapping("/{clientId}/orcamentos")
    @Operation(summary = "Listar todas as ordens de serviço com orçamento aberto de um cliente")
    public ResponseEntity<List<OrdemDeServicoDTO>> listarOrcamentos(@PathVariable Long clientId) {
        return ResponseEntity.ok(ordemService.listarOrcamentosAbertosPorCliente(clientId));
    }

    @PostMapping("/{id}/aprovar-orcamento")
    @Operation(summary = "Aprovar ou rejeitar um orçamento")
    public ResponseEntity<OrdemDeServicoDTO> aprovarOrcamento(@PathVariable Long id, @RequestHeader("status-orcamento") StatusOrcamento statusOrcamento) {
        return ResponseEntity.ok(ordemService.aprovarOrcamento(id, statusOrcamento));
    }

    @GetMapping("/{clientId}/concluidas")
    @Operation(summary = "Listar todas as ordens de serviço concluidas de um cliente")
    public ResponseEntity<List<OrdemDeServicoDTO>> listarOsConcluidasPorCliente(@PathVariable Long clientId) {
        return ResponseEntity.ok(ordemService.listarOsConcluidasPorCliente(clientId));
    }

    @PostMapping("/{id}/finalizar-entrega")
    @Operation(summary = "Finaliza uma OS com status ENTREGUE")
    public ResponseEntity<OrdemDeServicoDTO> finalizarOsEntregue(@PathVariable Long id) {
        return ResponseEntity.ok(ordemService.finalizarOsEntregue(id));
    }

    @GetMapping
    @Operation(summary = "Listar todas as ordens de serviço")
    public ResponseEntity<List<OrdemDeServicoDTO>> listarOrdens() {
        return ResponseEntity.ok(ordemService.listarOrdens());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar ordem de serviço por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OS encontrada"),
            @ApiResponse(responseCode = "404", description = "OS não encontrada")
    })
    public ResponseEntity<OrdemDeServicoDTO> obterOrdemPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ordemService.obterOrdemPorId(id));
    }

    @GetMapping("/cliente/{clienteId}")
    @Operation(summary = "Listar ordens de serviço de um cliente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    })
    public ResponseEntity<List<OrdemDeServicoDTO>> listarPorCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(ordemService.listarOrdensPorCliente(clienteId));
    }

//    @PatchMapping("/{id}/status")
//    @Operation(summary = "Avançar status da ordem de serviço")
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "Status atualizado com sucesso"),
//            @ApiResponse(responseCode = "400", description = "Transição de status inválida"),
//            @ApiResponse(responseCode = "404", description = "OS não encontrada")
//    })
//    public ResponseEntity<OrdemDeServicoDTO> avancarStatus(
//            @PathVariable Long id,
//            @Valid @RequestBody AvancarStatusDTO dto) {
//        return ResponseEntity.ok(ordemService.avancarStatus(id, dto));
//    }

    @PostMapping("/{id}/itens")
    @Operation(summary = "Adicionar um ou mais serviços à ordem de serviço")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Itens adicionados com sucesso"),
            @ApiResponse(responseCode = "400", description = "Status não permite adição de itens ou lista vazia"),
            @ApiResponse(responseCode = "404", description = "OS ou serviço não encontrado")
    })
    public ResponseEntity<OrdemDeServicoDTO> adicionarItens(
            @PathVariable Long id,
            @Valid @RequestBody AdicionarItemDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ordemService.adicionarItens(id, dto));
    }

    @DeleteMapping("/{id}/itens/{itemId}")
    @Operation(summary = "Remover item da ordem de serviço")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item removido com sucesso"),
            @ApiResponse(responseCode = "400", description = "Status não permite remoção de itens"),
            @ApiResponse(responseCode = "404", description = "OS ou item não encontrado")
    })
    public ResponseEntity<OrdemDeServicoDTO> removerItem(
            @PathVariable Long id,
            @PathVariable Long itemId) {
        return ResponseEntity.ok(ordemService.removerItem(id, itemId));
    }

     @GetMapping("/{id}/status")
     @Operation(summary = "Consultar status atual da ordem de serviço")
     @ApiResponses({
             @ApiResponse(responseCode = "200", description = "Status retornado com sucesso"),
             @ApiResponse(responseCode = "404", description = "OS não encontrada")
     })
     public ResponseEntity<OrdemStatusDTO> obterStatus(@PathVariable Long id) {
         return ResponseEntity.ok(ordemService.obterStatus(id));
     }

     @GetMapping("/tempo/{dataInicial}/{dataFinal}")
     @Operation(summary = "Listar tempo médio de execução de serviço em um período")
     public ResponseEntity<TempoExecucaoDTO> listarTempoMedioPorPeriodo(
             @PathVariable("dataInicial") @NotNull(message = "Data Inicial deve ser informada") LocalDate dataInicial,
             @PathVariable("dataFinal") LocalDate dataFinal) {
         return ResponseEntity.ok(ordemService.listarTempoMedioPorPeriodo(dataInicial, dataFinal));
     }

    @GetMapping("/{sagaId}/historico")
    public ResponseEntity<List<SagaHistorico>> consultarHistoricoSaga(@PathVariable String sagaId) {
        List<SagaHistorico> historico = historicoRepository.findBySagaIdOrderByOccurredAtAsc(sagaId);
        return ResponseEntity.ok(historico);
    }


}
