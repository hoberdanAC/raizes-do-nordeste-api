package br.com.raizesdonordeste.application.port;

import br.com.raizesdonordeste.domain.enums.MetodoPagamento;
import java.math.BigDecimal;

/**
 * Porta para o serviço EXTERNO de pagamento. Hoje a implementação é um mock
 * (GatewayPagamentoMock); trocar por um provedor real exigiria apenas outra implementação,
 * sem mexer nos casos de uso.
 */
public interface GatewayPagamento {

    /**
     * Envia a cobrança ao gateway.
     * @throws GatewayIndisponivelException se o provedor não responder (timeout/queda).
     */
    RespostaGateway processar(SolicitacaoPagamento solicitacao);

    /** Payload enviado ao gateway. */
    record SolicitacaoPagamento(String referencia, BigDecimal valor, MetodoPagamento metodo, String tokenPagamento) { }

    /** Payload devolvido pelo gateway. */
    record RespostaGateway(boolean aprovado, String codigoTransacao, String mensagem) { }
}
