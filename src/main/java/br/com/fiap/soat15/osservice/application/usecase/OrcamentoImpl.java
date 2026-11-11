package br.com.fiap.soat15.osservice.application.usecase;

import br.com.fiap.soat15.osservice.adapter.out.gateway.dto.BillingDto;
import br.com.fiap.soat15.osservice.adapter.out.persistence.repository.ItemOSRepository;
import br.com.fiap.soat15.osservice.adapter.out.persistence.repository.OrdemDeServicoRepository;
import br.com.fiap.soat15.osservice.adapter.out.saga.out.SqsBillingService;
import br.com.fiap.soat15.osservice.application.dto.ItemOSDTO;
import br.com.fiap.soat15.osservice.domain.entity.*;
import br.com.fiap.soat15.osservice.domain.service.OrcamentoService;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Service
@AllArgsConstructor
public class OrcamentoImpl implements OrcamentoService {

    private final ItemOSRepository itemOSRepository;
    private final OrdemDeServicoRepository osRepository;
    private final SqsBillingService sqsProducer;

    @Override
    public void solicitarOrcamento(OrdemDeServico ordem, Long clienteId) {

        List<ItemOS> itemOSList = itemOSRepository.buscarPorOrdemComServico(ordem);
        if (itemOSList.isEmpty()) {
            throw new BadCredentialsException("Não é possivel solicitar orçamento para uma OS sem itens ou serviços.");
        }

        String sagaId = ordem.getSagaId() != null ? ordem.getSagaId() : "saga-" + ordem.getId();

        Set<Servico> servicos = ordem.getItens().stream()
                .map(ItemOS::getServico)
                .collect(Collectors.toSet());

        itemOSList.addAll(servicos.stream()
                .map(servico -> this.toServicoDTO(servico, ordem))
                .toList());

        List<BillingDto.ItemOrcamentoRequest> itensRequest = itemOSList.stream()
                .map(item -> new BillingDto.ItemOrcamentoRequest(
                        item.getIdOrigemItem(),
                        item.getTipoItem().toString(),
                        item.getNomeItem(),
                        item.getQuantidade(),
                        item.getPrecoUnitario()
                ))
                .collect(toList());

        // 4. Monta o comando de requisição para o Billing[cite: 3]
        String comandoId = UUID.randomUUID().toString();
        BillingDto.GerarOrcamentoRequest billingRequest = new BillingDto.GerarOrcamentoRequest(
                UUID.randomUUID().toString(),
                sagaId,
                "gerar-orcamento-os-" + ordem.getId(),
                comandoId,
                ordem.getId(),
                clienteId,
                itensRequest
        );

        ordem.setSagaId(sagaId);
        ordem.setStatusSaga(StatusSaga.ENVIADO_PARA_ORCAMENTO);
        OrdemDeServico os = osRepository.save(ordem);

        sqsProducer.enviarComandoGerarOrcamento(billingRequest);

    }

    @Override
    public void processarPagamento(OrdemDeServico ordem, Long clienteId){

        BillingDto.ProcessarPagamentoRequest pagamentoRequest = new BillingDto.ProcessarPagamentoRequest(
                UUID.randomUUID().toString(),
                ordem.getSagaId(),
                "processar-pagamento-os-" + ordem.getId(),
                UUID.randomUUID().toString(),
                ordem.getId(),
                clienteId
        );

        sqsProducer.enviarComandoProcessarPagamento(pagamentoRequest);

        ordem.setStatusSaga(StatusSaga.ENVIADO_PARA_PAGAMENTO);
        osRepository.save(ordem);
    }

    private ItemOS toServicoDTO(Servico servico, OrdemDeServico ordem) {
        return ItemOS.builder()
                .id(servico.getId())
                .ordemDeServico(ordem)
                .servico(servico)
                .quantidade(1)
                .precoUnitario(servico.getPreco())
                .subtotal(servico.getPreco())
                .nomeItem(servico.getNome())
                .idOrigemItem(servico.getIdOrigem())
                .tipoItem(TipoItem.SERVICO)
                .build();
    }
}
