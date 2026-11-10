package br.com.raizesdonordeste.domain.exception;

/**
 * Usuário autenticado tentando acessar dado que não é dele (ex.: pedido de outro cliente
 * ou de outra unidade). Traduzida para HTTP 403.
 */
public class AcessoNegadoException extends DominioException {
    public AcessoNegadoException(String mensagem) {
        super("ACESSO_NEGADO", mensagem, null);
    }
}
