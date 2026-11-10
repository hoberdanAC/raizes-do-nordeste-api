package br.com.raizesdonordeste.infrastructure.repository;

import br.com.raizesdonordeste.domain.model.MovimentacaoEstoque;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Long> {
}
