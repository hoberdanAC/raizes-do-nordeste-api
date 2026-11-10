package br.com.raizesdonordeste.infrastructure.repository;

import br.com.raizesdonordeste.domain.enums.StatusPagamento;
import br.com.raizesdonordeste.domain.model.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {
    List<Pagamento> findByPedidoIdOrderByCriadoEmAsc(Long pedidoId);
    Optional<Pagamento> findFirstByPedidoIdAndStatus(Long pedidoId, StatusPagamento status);
}
