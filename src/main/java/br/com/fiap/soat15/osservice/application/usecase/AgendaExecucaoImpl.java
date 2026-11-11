package br.com.fiap.soat15.osservice.application.usecase;

import br.com.fiap.soat15.osservice.adapter.out.persistence.repository.ItemOSRepository;
import br.com.fiap.soat15.osservice.adapter.out.persistence.repository.OrdemDeServicoRepository;
import br.com.fiap.soat15.osservice.adapter.out.persistence.repository.SagaHistoricoRepository;
import br.com.fiap.soat15.osservice.adapter.out.saga.dto.AgendarExecucaoCommandDto;
import br.com.fiap.soat15.osservice.adapter.out.saga.dto.BillingResponse;
import br.com.fiap.soat15.osservice.adapter.out.saga.dto.EnvelopeResponseSQS;
import br.com.fiap.soat15.osservice.adapter.out.saga.out.SqsExecucaoService;
import br.com.fiap.soat15.osservice.application.dto.AvancarStatusDTO;
import br.com.fiap.soat15.osservice.domain.entity.*;
import br.com.fiap.soat15.osservice.domain.service.AgendaExecucaoService;
import br.com.fiap.soat15.osservice.domain.service.OrdemDeServicoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgendaExecucaoImpl implements AgendaExecucaoService {

    private final OrdemDeServicoRepository osRepository;
    private final SqsExecucaoService sqsProducer;
    private final ItemOSRepository itemRepository;
    private final SagaHistoricoRepository sagaHistoricoRepository;
    private final OrdemDeServicoService osService;

    @Transactional
    public void executar(BillingResponse.PagamentoResponseDto pagamentoResponseDto) {

        Long osId = pagamentoResponseDto.ordemId();

        if (!osRepository.existsByIdAndStatus(osId, StatusOS.APROVADA))
            throw new BadCredentialsException("Não é possível solicitar execução para uma OS sem pagamento OS: " + osId);

        OrdemDeServico os = osService.avancarStatus(osId, AvancarStatusDTO.builder()
                .novoStatus(StatusOS.EM_EXECUCAO)
                .observacoes("Pagamento aprovado com sucesso.")
                .statusSaga(StatusSaga.EXECUCAO_AGENDADA)
                .pagamentoId(pagamentoResponseDto.pagamentoId())
                .build());

        AgendarExecucaoCommandDto comando = new AgendarExecucaoCommandDto(
                new EnvelopeResponseSQS(
                        1, // schemaVersion
                        UUID.randomUUID().toString(), // messageId único para a mensagem
                        "agendar-os-" + osId + "-pagamento-" + pagamentoResponseDto.pagamentoId(), // operationId único para deduplicação
                        "AgendarExecucao", // type obrigatório
                        os.getSagaId(),
                        osId.toString(),
                        pagamentoResponseDto.pagamentoId(), // causationId amarrado ao pagamento
                        Instant.now()
                ),
                new AgendarExecucaoCommandDto.PayloadDto(
                        pagamentoResponseDto.orcamentoId(),
                        pagamentoResponseDto.versao(), // versao do orçamento
                        pagamentoResponseDto.pagamentoId(),
                        getItemCommandDto(os)
                )
        );

        sqsProducer.enviarComandoAgendamento(comando);
        salvarHistorico(comando.envelopeResponseSQS());

        System.out.println("Pagamento confirmado para a OS " + osId + ". Comando AgendarExecucao enviado ao SQS com sucesso!");
    }

    private List<AgendarExecucaoCommandDto.ItemCommandDto> getItemCommandDto(OrdemDeServico ordem) {
        List<AgendarExecucaoCommandDto.ItemCommandDto> list = new ArrayList<>();
        List<ItemOS> itens = itemRepository.findByOrdemDeServico(ordem);

        Set<Servico> servicos = ordem.getItens().stream()
                .map(ItemOS::getServico)
                .collect(Collectors.toSet());


        for (ItemOS item : itens) {
            AgendarExecucaoCommandDto.ItemCommandDto dto =
                    new AgendarExecucaoCommandDto.ItemCommandDto(
                            item.getTipoItem().toString(),
                            item.getIdOrigemItem(),
                            item.getQuantidade()
                    );
            list.add(dto);
        }

        for (Servico servico : servicos) {
            AgendarExecucaoCommandDto.ItemCommandDto dto =
                    new AgendarExecucaoCommandDto.ItemCommandDto(
                            TipoItem.SERVICO.toString(),
                            servico.getIdOrigem(),
                            1
                    );
            list.add(dto);
        }

        return list;
    }

    private void salvarHistorico(EnvelopeResponseSQS envelopeSqs) {
        SagaHistorico historico = new SagaHistorico();
        historico.setSagaId(envelopeSqs.sagaId());
        historico.setOsId(envelopeSqs.osId());
        historico.setMessageId(envelopeSqs.messageId());
        historico.setType(envelopeSqs.type());
        historico.setOperationId(envelopeSqs.operationId());
        historico.setOccurredAt(Instant.now());

        sagaHistoricoRepository.save(historico);
    }
}