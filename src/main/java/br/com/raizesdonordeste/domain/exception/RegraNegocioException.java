package br.com.raizesdonordeste.domain.exception;

import java.util.List;

/** Conflito com regra de negócio (estoque insuficiente, status inválido...). Traduzida para HTTP 409. */
public class RegraNegocioException extends DominioException {
    public RegraNegocioException(String codigo, String mensagem) {
        super(codigo, mensagem, null);
    }

    public RegraNegocioException(String codigo, String mensagem, List<Violacao> violacoes) {
        super(codigo, mensagem, violacoes);
    }
}
