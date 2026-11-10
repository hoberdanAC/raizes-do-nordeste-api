package br.com.raizesdonordeste.application.port;

/**
 * Gateway respondeu, mas NEGOU o pagamento. Traduzida para HTTP 402 (Payment Required)
 * no formato de erro padrão. A tentativa recusada fica gravada no banco.
 */
public class PagamentoRecusadoException extends RuntimeException {

    private final Long pagamentoId;
    private final Long pedidoId;

    public PagamentoRecusadoException(String mensagem, Long pagamentoId, Long pedidoId) {
        super(mensagem);
        this.pagamentoId = pagamentoId;
        this.pedidoId = pedidoId;
    }

    public Long getPagamentoId() { return pagamentoId; }
    public Long getPedidoId() { return pedidoId; }
}
