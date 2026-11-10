package br.com.raizesdonordeste.domain.enums;

/** Tipos de lançamento no extrato de pontos de fidelidade. */
public enum TipoTransacaoFidelidade {
    ACUMULO,  // pontos ganhos em pedido pago
    RESGATE,  // pontos trocados por benefício
    ESTORNO   // pontos removidos porque o pedido foi cancelado
}
