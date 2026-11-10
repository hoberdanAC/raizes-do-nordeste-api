package br.com.raizesdonordeste.infrastructure.repository;

import br.com.raizesdonordeste.domain.enums.CanalPedido;
import br.com.raizesdonordeste.domain.enums.StatusPedido;
import br.com.raizesdonordeste.domain.model.Pedido;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros reutilizáveis da listagem de pedidos. Cada método devolve null quando o
 * parâmetro não foi informado (o Spring ignora specs nulas ao combinar com and()).
 */
public final class PedidoSpecs {

    private PedidoSpecs() { }

    /** Filtro exigido pelo roteiro: GET /pedidos?canalPedido=TOTEM */
    public static Specification<Pedido> canal(CanalPedido canal) {
        return canal == null ? null : (root, q, cb) -> cb.equal(root.get("canalPedido"), canal);
    }

    public static Specification<Pedido> status(StatusPedido status) {
        return status == null ? null : (root, q, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Pedido> unidade(Long unidadeId) {
        return unidadeId == null ? null : (root, q, cb) -> cb.equal(root.get("unidade").get("id"), unidadeId);
    }

    public static Specification<Pedido> cliente(Long clienteId) {
        return clienteId == null ? null : (root, q, cb) -> cb.equal(root.get("cliente").get("id"), clienteId);
    }
}
