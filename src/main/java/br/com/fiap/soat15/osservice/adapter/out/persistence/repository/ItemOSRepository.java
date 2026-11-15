package br.com.fiap.soat15.osservice.adapter.out.persistence.repository;

import br.com.fiap.soat15.osservice.domain.entity.ItemOS;
import br.com.fiap.soat15.osservice.domain.entity.OrdemDeServico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemOSRepository extends JpaRepository<ItemOS, Long> {
    List<ItemOS> findByOrdemDeServico(OrdemDeServico ordem);

    @Query("""
    SELECT i
    FROM ItemOS i
    JOIN FETCH i.servico
    WHERE i.ordemDeServico = :ordem
""")
    List<ItemOS> buscarPorOrdemComServico(
            @Param("ordem") OrdemDeServico ordem
    );
    List<ItemOS> findByOrdemDeServicoId(Long id);
}
