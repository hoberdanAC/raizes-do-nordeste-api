package br.com.raizesdonordeste.domain.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Ciclo de vida do pedido. A máquina de estados fica no domínio para que nenhuma
 * camada consiga "pular" etapas (ex.: ENTREGUE sem passar por PRONTO).
 *
 * AGUARDANDO_PAGAMENTO -> PAGO -> EM_PREPARACAO -> PRONTO -> ENTREGUE
 *            \             \
 *             +-> CANCELADO +-> CANCELADO
 */
public enum StatusPedido {
    AGUARDANDO_PAGAMENTO,
    PAGO,
    EM_PREPARACAO,
    PRONTO,
    ENTREGUE,
    CANCELADO;

    /** Retorna os próximos status permitidos a partir do status atual. */
    public Set<StatusPedido> proximosPermitidos() {
        return switch (this) {
            case AGUARDANDO_PAGAMENTO -> EnumSet.of(PAGO, CANCELADO);
            case PAGO -> EnumSet.of(EM_PREPARACAO, CANCELADO);
            case EM_PREPARACAO -> EnumSet.of(PRONTO);
            case PRONTO -> EnumSet.of(ENTREGUE);
            case ENTREGUE, CANCELADO -> EnumSet.noneOf(StatusPedido.class); // estados finais
        };
    }

    public boolean podeIrPara(StatusPedido novo) {
        return proximosPermitidos().contains(novo);
    }
}
