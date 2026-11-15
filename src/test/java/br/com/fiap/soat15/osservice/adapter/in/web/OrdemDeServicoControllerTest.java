package br.com.fiap.soat15.osservice.adapter.in.web;

import br.com.fiap.soat15.osservice.adapter.out.persistence.repository.SagaHistoricoRepository;
import br.com.fiap.soat15.osservice.application.dto.*;
import br.com.fiap.soat15.osservice.config.security.JwtAuthFilter;
import br.com.fiap.soat15.osservice.domain.entity.StatusOrcamento;
import br.com.fiap.soat15.osservice.domain.service.OrdemDeServicoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrdemDeServicoController.class)
class OrdemDeServicoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private OrdemDeServicoService ordemService;

    @MockitoBean
    private SagaHistoricoRepository historicoRepository;

    @Test
    void deveCriarOrdemComSucesso() throws Exception {
        CriarOrdemDTO requestDto = CriarOrdemDTO.builder()
                .veiculoId(1L).clienteId(1L).descricaoProblema("carro velho").build();
        OrdemDeServicoDTO responseDto = OrdemDeServicoDTO.builder()
                .veiculoId(1L).clienteId(1L).descricaoProblema("carro velho").build();


        when(ordemService.criarOrdem(any(CriarOrdemDTO.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/ordens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk());
    }

    @Test
    void deveSolicitarOrcamentoComSucesso() throws Exception {
        Long id = 1L;
        when(ordemService.solicitarOrcamento(id)).thenReturn(new OrdemDeServicoDTO());

        mockMvc.perform(post("/api/v1/ordens/{id}/orcamento", id))
                .andExpect(status().isOk());
    }

    @Test
    void deveListarOrcamentosAbertosPorCliente() throws Exception {
        Long clientId = 1L;
        when(ordemService.listarOrcamentosAbertosPorCliente(clientId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/ordens/{clientId}/orcamentos", clientId))
                .andExpect(status().isOk());
    }

    @Test
    void deveAprovarOrcamentoComSucesso() throws Exception {
        Long id = 1L;
        when(ordemService.aprovarOrcamento(eq(id), eq(StatusOrcamento.APROVADO))).thenReturn(new OrdemDeServicoDTO());

        mockMvc.perform(post("/api/v1/ordens/{id}/aprovar-orcamento", id)
                        .header("status-orcamento", "APROVADO"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void deveListarOsConcluidasPorCliente() throws Exception {
        Long clientId = 1L;
        when(ordemService.listarOsConcluidasPorCliente(clientId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/ordens/{clientId}/concluidas", clientId))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void deveFinalizarOsEntregueComSucesso() throws Exception {
        Long id = 1L;
        when(ordemService.finalizarOsEntregue(id)).thenReturn(new OrdemDeServicoDTO());

        mockMvc.perform(post("/api/v1/ordens/{id}/finalizar-entrega", id))
                .andExpect(status().isOk());
    }

    @Test
    void deveCancelarOsComSucesso() throws Exception {
        Long id = 1L;
        doNothing().when(ordemService).cancelarOrdem(id);

        mockMvc.perform(post("/api/v1/ordens/{id}/cancelar-os", id))
                .andExpect(status().isOk());
    }

    @Test
    void deveListarTodasAsOrdens() throws Exception {
        when(ordemService.listarOrdens()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/ordens"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void deveObterOrdemPorIdComSucesso() throws Exception {
        Long id = 1L;
        when(ordemService.obterOrdemPorId(id)).thenReturn(new OrdemDeServicoDTO());

        mockMvc.perform(get("/api/v1/ordens/{id}", id))
                .andExpect(status().isOk());
    }

    @Test
    void deveListarOrdensPorCliente() throws Exception {
        Long clienteId = 1L;
        when(ordemService.listarOrdensPorCliente(clienteId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/ordens/cliente/{clienteId}", clienteId))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void deveAdicionarItensComSucesso() throws Exception {
        Long id = 1L;
        AdicionarItemDTO requestDto = new AdicionarItemDTO();
        when(ordemService.adicionarItens(eq(id), any(AdicionarItemDTO.class))).thenReturn(new OrdemDeServicoDTO());

        mockMvc.perform(post("/api/v1/ordens/{id}/itens", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk());
    }

    @Test
    void deveRemoverItemComSucesso() throws Exception {
        Long id = 1L;
        String itemId = "item-123";
        when(ordemService.removerItem(id, itemId)).thenReturn(new OrdemDeServicoDTO());

        mockMvc.perform(delete("/api/v1/ordens/{id}/itens/{itemId}", id, itemId))
                .andExpect(status().isOk());
    }

    @Test
    void deveObterStatusComSucesso() throws Exception {
        Long id = 1L;
        when(ordemService.obterStatus(id)).thenReturn(new OrdemStatusDTO());

        mockMvc.perform(get("/api/v1/ordens/{id}/status", id))
                .andExpect(status().isOk());
    }

    @Test
    void deveListarTempoMedioPorPeriodoComSucesso() throws Exception {
        LocalDate inicio = LocalDate.now().minusDays(10);
        LocalDate fim = LocalDate.now();
        when(ordemService.listarTempoMedioPorPeriodo(any(LocalDate.now().getClass()), any(LocalDate.now().getClass())))
                .thenReturn(new TempoExecucaoDTO());

        mockMvc.perform(get("/api/v1/ordens/tempo/{dataInicial}/{dataFinal}", inicio, fim))
                .andExpect(status().isOk());
    }

    @Test
    void deveConsultarHistoricoSagaComSucesso() throws Exception {
        String sagaId = "uuid-saga-123";
        when(historicoRepository.findBySagaIdOrderByOccurredAtAsc(sagaId))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/ordens/{sagaId}/historico", sagaId))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }
}