package br.com.raizesdonordeste.infrastructure.payment;

import br.com.raizesdonordeste.application.port.GatewayIndisponivelException;
import br.com.raizesdonordeste.application.port.GatewayPagamento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Simulação (mock) do provedor externo de pagamento. NÃO processa pagamento real.
 *
 * Regras determinísticas para permitir testes reproduzíveis no Postman:
 *  - tokenPagamento = "tok_recusado" -> RECUSADO (saldo insuficiente)
 *  - tokenPagamento = "tok_falha"    -> lança GatewayIndisponivelException (simula timeout)
 *  - valor acima de R$ 2.000,00      -> RECUSADO (limite excedido)
 *  - qualquer outro caso             -> APROVADO com código de transação
 */
@Component
public class GatewayPagamentoMock implements GatewayPagamento {

    private static final Logger log = LoggerFactory.getLogger(GatewayPagamentoMock.class);
    private static final BigDecimal LIMITE = new BigDecimal("2000.00");

    @Override
    public RespostaGateway processar(SolicitacaoPagamento s) {
        log.info("[GATEWAY MOCK] Requisição recebida: ref={} valor={} metodo={}", s.referencia(), s.valor(), s.metodo());

        if ("tok_falha".equals(s.tokenPagamento())) {
            throw new GatewayIndisponivelException("Timeout ao comunicar com o gateway (simulado).");
        }

        String codigo = "MOCK-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();

        if ("tok_recusado".equals(s.tokenPagamento())) {
            return new RespostaGateway(false, codigo, "Pagamento recusado pelo emissor: saldo insuficiente.");
        }
        if (s.valor().compareTo(LIMITE) > 0) {
            return new RespostaGateway(false, codigo, "Pagamento recusado: valor acima do limite permitido.");
        }

        String mensagem = switch (s.metodo()) {
            case PIX -> "PIX confirmado.";
            case CREDITO -> "Crédito autorizado.";
            case DEBITO -> "Débito autorizado.";
        };
        return new RespostaGateway(true, codigo, mensagem);
    }
}
