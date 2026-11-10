package br.com.raizesdonordeste.infrastructure.repository;

import br.com.raizesdonordeste.domain.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;

/** JpaSpecificationExecutor permite montar filtros dinâmicos (?canalPedido=&status=&unidadeId=). */
public interface PedidoRepository extends JpaRepository<Pedido, Long>, JpaSpecificationExecutor<Pedido> {
    Optional<Pedido> findByCriadoPorIdAndChaveIdempotencia(Long criadoPorId, String chaveIdempotencia);
}
