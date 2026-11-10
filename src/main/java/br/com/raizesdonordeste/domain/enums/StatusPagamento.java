package br.com.raizesdonordeste.domain.enums;

/** Resultado de uma tentativa de pagamento junto ao gateway. */
public enum StatusPagamento {
    APROVADO,
    RECUSADO,   // gateway respondeu negando (ex.: saldo insuficiente)
    ERRO,       // gateway indisponível / timeout (tolerância a falhas)
    ESTORNADO   // pagamento aprovado que foi devolvido no cancelamento
}
