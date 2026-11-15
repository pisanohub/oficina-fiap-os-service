//package br.com.fiap.soat15.osservice.adapter.in.web;
//
//import br.com.fiap.soat15.osservice.adapter.out.persistence.repository.SagaHistoricoRepository;
//import br.com.fiap.soat15.osservice.application.dto.AdicionarItemDTO;
//import br.com.fiap.soat15.osservice.application.dto.CriarOrdemDTO;
//import br.com.fiap.soat15.osservice.config.security.JwtAuthFilter;
//import br.com.fiap.soat15.osservice.config.security.JwtService;
//import br.com.fiap.soat15.osservice.config.security.UsuarioDetailsService;
//import br.com.fiap.soat15.osservice.domain.entity.StatusOrcamento;
//import br.com.fiap.soat15.osservice.domain.service.OrdemDeServicoService;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.DisplayName;
//import org.springframework.context.annotation.Import;
//import org.springframework.http.MediaType;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
//import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
//import org.springframework.test.context.bean.override.BeanOverride;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.time.LocalDate;
//import java.util.Collections;
//
//import static org.mockito.Mockito.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//import static org.mockito.ArgumentMatchers.any;
//
//@WebMvcTest(OrdemDeServicoController.class)
//@AutoConfigureMockMvc
//class OrdemDeServicoControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @MockitoBean
//    private OrdemDeServicoService ordemService;
//
//    @MockitoBean
//    private SagaHistoricoRepository historicoRepository;
//
//    @BeanOverride // ou @MockBean dependendo da versão do Spring Boot
//    private UsuarioDetailsService usuarioDetailsService;
//
//
//    private static final Long ORDEM_ID = 1L;
//    private static final Long CLIENTE_ID = 10L;
//    private static final String SAGA_ID = "saga-teste-123";
//
//    // Os testes abaixo verificam apenas o comportamento HTTP
//    // e a delegação para o Service mockado.
//
//    @Test
//    @DisplayName("POST /api/v1/ordens - Deve criar uma ordem de serviço")
//    void deveCriarOrdem() throws Exception {
//        String json = """
//                {
//                    "clienteId": 10,
//                    "veiculoId": 1,
//                    "descricaoProblema": "Problema no motor",
//                    "observacoes": "Verificar ruído"
//                }
//                """;
//
//        mockMvc.perform(post("/api/v1/ordens")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(json))
//                .andExpect(status().isCreated());
//
//        verify(ordemService).criarOrdem(any(CriarOrdemDTO.class));
//    }
//
//    @Test
//    @DisplayName("POST /api/v1/ordens - Deve retornar 400 para JSON inválido")
//    void deveRetornar400AoCriarOrdemComJsonInvalido() throws Exception {
//        mockMvc.perform(post("/api/v1/ordens")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{"))
//                .andExpect(status().isBadRequest());
//
//        verifyNoInteractions(ordemService);
//    }
//
//    @Test
//    @DisplayName("POST /{id}/orcamento - Deve solicitar orçamento")
//    void deveSolicitarOrcamento() throws Exception {
//        mockMvc.perform(post("/api/v1/ordens/{id}/orcamento", ORDEM_ID))
//                .andExpect(status().isOk());
//
//        verify(ordemService).solicitarOrcamento(ORDEM_ID);
//    }
//
//    @Test
//    @DisplayName("GET /{clientId}/orcamentos - Deve listar orçamentos abertos")
//    void deveListarOrcamentos() throws Exception {
//        when(ordemService.listarOrcamentosAbertosPorCliente(CLIENTE_ID))
//                .thenReturn(Collections.emptyList());
//
//        mockMvc.perform(get("/api/v1/ordens/{clientId}/orcamentos", CLIENTE_ID))
//                .andExpect(status().isOk())
//                .andExpect(content().json("[]"));
//
//        verify(ordemService).listarOrcamentosAbertosPorCliente(CLIENTE_ID);
//    }
//
//    @Test
//    @DisplayName("POST /{id}/aprovar-orcamento - Deve aprovar orçamento")
//    void deveAprovarOrcamento() throws Exception {
//        mockMvc.perform(post("/api/v1/ordens/{id}/aprovar-orcamento", ORDEM_ID)
//                        .header("status-orcamento", StatusOrcamento.APROVADO.name()))
//                .andExpect(status().isOk());
//
//        verify(ordemService).aprovarOrcamento(ORDEM_ID, StatusOrcamento.APROVADO);
//    }
//
//    @Test
//    @DisplayName("GET /{clientId}/concluidas - Deve listar OS concluídas")
//    void deveListarOsConcluidas() throws Exception {
//        when(ordemService.listarOsConcluidasPorCliente(CLIENTE_ID))
//                .thenReturn(Collections.emptyList());
//
//        mockMvc.perform(get("/api/v1/ordens/{clientId}/concluidas", CLIENTE_ID))
//                .andExpect(status().isOk())
//                .andExpect(content().json("[]"));
//
//        verify(ordemService).listarOsConcluidasPorCliente(CLIENTE_ID);
//    }
//
//    @Test
//    @DisplayName("POST /{id}/finalizar-entrega - Deve finalizar entrega")
//    void deveFinalizarEntrega() throws Exception {
//        mockMvc.perform(post("/api/v1/ordens/{id}/finalizar-entrega", ORDEM_ID))
//                .andExpect(status().isOk());
//
//        verify(ordemService).finalizarOsEntregue(ORDEM_ID);
//    }
//
//    @Test
//    @DisplayName("POST /{id}/cancelar-os - Deve solicitar cancelamento")
//    void deveCancelarOs() throws Exception {
//        mockMvc.perform(post("/api/v1/ordens/{id}/cancelar-os", ORDEM_ID))
//                .andExpect(status().isOk());
//
//        verify(ordemService).cancelarOrdem(ORDEM_ID);
//    }
//
//    @Test
//    @DisplayName("GET /api/v1/ordens - Deve listar todas as ordens")
//    void deveListarOrdens() throws Exception {
//        when(ordemService.listarOrdens())
//                .thenReturn(Collections.emptyList());
//
//        mockMvc.perform(get("/api/v1/ordens"))
//                .andExpect(status().isOk())
//                .andExpect(content().json("[]"));
//
//        verify(ordemService).listarOrdens();
//    }
//
//    @Test
//    @DisplayName("GET /{id} - Deve buscar ordem por ID")
//    void deveObterOrdemPorId() throws Exception {
//        mockMvc.perform(get("/api/v1/ordens/{id}", ORDEM_ID))
//                .andExpect(status().isOk());
//
//        verify(ordemService).obterOrdemPorId(ORDEM_ID);
//    }
//
//    @Test
//    @DisplayName("GET /cliente/{clienteId} - Deve listar ordens por cliente")
//    void deveListarOrdensPorCliente() throws Exception {
//        when(ordemService.listarOrdensPorCliente(CLIENTE_ID))
//                .thenReturn(Collections.emptyList());
//
//        mockMvc.perform(get("/api/v1/ordens/cliente/{clienteId}", CLIENTE_ID))
//                .andExpect(status().isOk())
//                .andExpect(content().json("[]"));
//
//        verify(ordemService).listarOrdensPorCliente(CLIENTE_ID);
//    }
//
//    @Test
//    @DisplayName("POST /{id}/itens - Deve adicionar itens à OS")
//    void deveAdicionarItens() throws Exception {
//        String json = """
//                {
//                    "itens": [
//                        {
//                            "servicoId": "SERVICO-001",
//                            "itemEstoqueId": "PECA-001",
//                            "tipoItem": "PECA",
//                            "quantidade": 2
//                        }
//                    ]
//                }
//                """;
//
//        mockMvc.perform(post("/api/v1/ordens/{id}/itens", ORDEM_ID)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(json))
//                .andExpect(status().isCreated());
//
//        verify(ordemService).adicionarItens(
//                eq(ORDEM_ID),
//                any(AdicionarItemDTO.class)
//        );
//    }
//
//    @Test
//    @DisplayName("DELETE /{id}/itens/{itemId} - Deve remover item")
//    void deveRemoverItem() throws Exception {
//        String itemId = "PECA-001";
//
//        mockMvc.perform(delete("/api/v1/ordens/{id}/itens/{itemId}", ORDEM_ID, itemId))
//                .andExpect(status().isOk());
//
//        verify(ordemService).removerItem(ORDEM_ID, itemId);
//    }
//
//    @Test
//    @DisplayName("GET /{id}/status - Deve consultar status da OS")
//    void deveObterStatus() throws Exception {
//        mockMvc.perform(get("/api/v1/ordens/{id}/status", ORDEM_ID))
//                .andExpect(status().isOk());
//
//        verify(ordemService).obterStatus(ORDEM_ID);
//    }
//
//    @Test
//    @DisplayName("GET /tempo/{dataInicial}/{dataFinal} - Deve consultar tempo médio")
//    void deveListarTempoMedioPorPeriodo() throws Exception {
//        LocalDate dataInicial = LocalDate.of(2026, 1, 1);
//        LocalDate dataFinal = LocalDate.of(2026, 1, 31);
//
//        mockMvc.perform(get(
//                        "/api/v1/ordens/tempo/{dataInicial}/{dataFinal}",
//                        dataInicial,
//                        dataFinal))
//                .andExpect(status().isOk());
//
//        verify(ordemService).listarTempoMedioPorPeriodo(dataInicial, dataFinal);
//    }
//
//    @Test
//    @DisplayName("GET /{sagaId}/historico - Deve consultar histórico da SAGA")
//    void deveConsultarHistoricoSaga() throws Exception {
//        when(historicoRepository.findBySagaIdOrderByOccurredAtAsc(SAGA_ID))
//                .thenReturn(Collections.emptyList());
//
//        mockMvc.perform(get("/api/v1/ordens/{sagaId}/historico", SAGA_ID))
//                .andExpect(status().isOk())
//                .andExpect(content().json("[]"));
//
//        verify(historicoRepository).findBySagaIdOrderByOccurredAtAsc(SAGA_ID);
//    }
//}