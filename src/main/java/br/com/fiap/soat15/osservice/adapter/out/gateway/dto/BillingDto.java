package br.com.fiap.soat15.osservice.adapter.out.gateway.dto;

import java.math.BigDecimal;
import java.util.List;

public class BillingDto {


    // Objeto enviado para gerar o orçamento com base no snapshot do OS
    public record GerarOrcamentoRequest(
            String messageId,
            String sagaId,
            String causationId,
            String comandoId,
            Long ordemId,
            Long clienteId,
            List<ItemOrcamentoRequest> itens
    ) {}

    public record ItemOrcamentoRequest(
            String referencia,  // Ex: "filtro-oleo" (equivalente ao itemId)
            String tipo,        // "PECA" ou "SERVICO"
            String descricao,   // Nome/descrição capturada no catálogo
            int quantidade,
            BigDecimal valorUnitario // Preço congelado no momento da adição
    ) {}

    // Resposta devolvida pelo Billing após persistir o orçamento[cite: 3]
    public record ProcessarPagamentoRequest(
            String messageId,
            String sagaId,
            String causationId,
            String comandoId,
            Long ordemId,
            Long clienteId,
            String type
    ) {}

}