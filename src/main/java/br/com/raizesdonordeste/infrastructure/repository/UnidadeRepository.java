package br.com.raizesdonordeste.infrastructure.repository;

import br.com.raizesdonordeste.domain.model.Unidade;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnidadeRepository extends JpaRepository<Unidade, Long> {
}
