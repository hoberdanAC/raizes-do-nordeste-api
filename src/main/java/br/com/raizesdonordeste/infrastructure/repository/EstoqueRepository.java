package br.com.raizesdonordeste.infrastructure.repository;

import br.com.raizesdonordeste.domain.model.Estoque;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface EstoqueRepository extends JpaRepository<Estoque, Long> {

    Optional<Estoque> findByUnidadeIdAndProdutoId(Long unidadeId, Long produtoId);

    /**
     * Busca com LOCK pessimista (SELECT ... FOR UPDATE). Em horário de pico, dois pedidos
     * simultâneos não conseguem vender a mesma última unidade do produto.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Estoque e where e.unidade.id = :unidadeId and e.produto.id = :produtoId")
    Optional<Estoque> buscarParaAtualizacao(@Param("unidadeId") Long unidadeId, @Param("produtoId") Long produtoId);

    /** Cardápio/saldo da unidade com o produto já carregado (evita N+1 consultas). */
    @Query("select e from Estoque e join fetch e.produto p where e.unidade.id = :unidadeId order by p.nome")
    List<Estoque> listarPorUnidade(@Param("unidadeId") Long unidadeId);
}
