package br.com.raizesdonordeste.domain.exception;

/** Recurso inexistente (pedido, produto, unidade...). Traduzida para HTTP 404. */
public class RecursoNaoEncontradoException extends DominioException {
    public RecursoNaoEncontradoException(String codigo, String mensagem) {
        super(codigo, mensagem, null);
    }
}
