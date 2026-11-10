package br.com.raizesdonordeste.infrastructure.repository;

import br.com.raizesdonordeste.domain.enums.TipoTransacaoFidelidade;
import br.com.raizesdonordeste.domain.model.TransacaoFidelidade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface TransacaoFidelidadeRepository extends JpaRepository<TransacaoFidelidade, Long> {

    /** Saldo = soma de todos os lançamentos (acúmulos positivos, resgates/estornos negativos). */
    @Query("select coalesce(sum(t.pontos), 0) from TransacaoFidelidade t where t.usuario.id = :usuarioId")
    Long calcularSaldo(@Param("usuarioId") Long usuarioId);

    Page<TransacaoFidelidade> findByUsuarioIdOrderByCriadoEmDesc(Long usuarioId, Pageable pageable);

    Optional<TransacaoFidelidade> findFirstByPedidoIdAndTipo(Long pedidoId, TipoTransacaoFidelidade tipo);
}
