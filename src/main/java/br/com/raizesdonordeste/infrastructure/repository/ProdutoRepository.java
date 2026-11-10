package br.com.raizesdonordeste.infrastructure.repository;

import br.com.raizesdonordeste.domain.model.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    Page<Produto> findByAtivoTrue(Pageable pageable);
}
