package br.com.fiap.soat15.osservice.adapter.out.persistence.repository;

import br.com.fiap.soat15.osservice.domain.entity.ItemOS;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemOSRepository extends JpaRepository<ItemOS, Long> {
}
