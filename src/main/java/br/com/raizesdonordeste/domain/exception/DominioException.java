package br.com.raizesdonordeste.domain.exception;

import java.util.List;

/**
 * Exceção base do domínio. Carrega um código estável (ex.: ESTOQUE_INSUFICIENTE),
 * que o front-end pode tratar sem depender do texto da mensagem.
 * O domínio NÃO conhece HTTP: a tradução para status code é feita na camada API.
 */
public abstract class DominioException extends RuntimeException {

    private final String codigo;
    private final List<Violacao> violacoes;

    protected DominioException(String codigo, String mensagem, List<Violacao> violacoes) {
        super(mensagem);
        this.codigo = codigo;
        this.violacoes = violacoes == null ? List.of() : violacoes;
    }

    public String getCodigo() { return codigo; }
    public List<Violacao> getViolacoes() { return violacoes; }
}
