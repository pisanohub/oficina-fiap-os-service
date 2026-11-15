package br.com.fiap.soat15.osservice.adapter.out.persistence.repository;

import br.com.fiap.soat15.osservice.domain.entity.SagaHistorico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SagaHistoricoRepository extends JpaRepository<SagaHistorico, Long> {
    boolean existsByMessageId(String messageId);

    List<SagaHistorico> findBySagaIdOrderByOccurredAtAsc(String sagaId);
}
