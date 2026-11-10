package br.com.raizesdonordeste.domain.exception;

import java.util.List;

/** Dado semanticamente inválido para a regra (ex.: canal BALCAO usado por cliente). HTTP 422. */
public class DadosInvalidosException extends DominioException {
    public DadosInvalidosException(String codigo, String mensagem, List<Violacao> violacoes) {
        super(codigo, mensagem, violacoes);
    }
}
