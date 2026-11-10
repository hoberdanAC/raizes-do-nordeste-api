package br.com.raizesdonordeste.application.port;

/** Falha de comunicação com o gateway (timeout, queda). Traduzida para HTTP 503. */
public class GatewayIndisponivelException extends RuntimeException {

    private final Long pagamentoId;

    public GatewayIndisponivelException(String mensagem) {
        this(mensagem, null);
    }

    public GatewayIndisponivelException(String mensagem, Long pagamentoId) {
        super(mensagem);
        this.pagamentoId = pagamentoId;
    }

    public Long getPagamentoId() { return pagamentoId; }
}
