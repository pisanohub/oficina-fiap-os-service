package br.com.fiap.soat15.osservice.adapter.out.persistence.repository;

import br.com.fiap.soat15.osservice.domain.entity.Servico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServicoRepository extends JpaRepository<Servico, Long> {
}
