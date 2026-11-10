package br.com.raizesdonordeste.application.service;

import br.com.raizesdonordeste.application.dto.PedidoDtos.PagamentoRequest;
import br.com.raizesdonordeste.application.dto.PedidoDtos.PagamentoResponse;
import br.com.raizesdonordeste.application.dto.UsuarioLogado;
import br.com.raizesdonordeste.application.port.GatewayIndisponivelException;
import br.com.raizesdonordeste.application.port.GatewayPagamento;
import br.com.raizesdonordeste.application.port.GatewayPagamento.RespostaGateway;
import br.com.raizesdonordeste.application.port.GatewayPagamento.SolicitacaoPagamento;
import br.com.raizesdonordeste.application.port.PagamentoRecusadoException;
import br.com.raizesdonordeste.domain.enums.StatusPagamento;
import br.com.raizesdonordeste.domain.enums.StatusPedido;
import br.com.raizesdonordeste.domain.exception.RegraNegocioException;
import br.com.raizesdonordeste.domain.model.Pagamento;
import br.com.raizesdonordeste.domain.model.Pedido;
import br.com.raizesdonordeste.infrastructure.repository.PagamentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Caso de uso: solicitar pagamento ao serviço externo (mock) e atualizar o pedido.
 * Resultados possíveis:
 *  - APROVADO  -> pedido vai para PAGO, pontos de fidelidade são creditados (201);
 *  - RECUSADO  -> tentativa gravada, pedido continua AGUARDANDO_PAGAMENTO (402);
 *  - ERRO      -> gateway fora do ar após 3 tentativas, pedido intacto (503);
 *  - pedido já pago não é cobrado de novo (idempotência, 409).
 */
@Service
public class PagamentoService {

    private static final Logger log = LoggerFactory.getLogger(PagamentoService.class);
    private static final int MAX_TENTATIVAS = 3;

    private final PedidoService pedidoService;
    private final PagamentoRepository pagamentoRepository;
    private final GatewayPagamento gateway;
    private final FidelidadeService fidelidadeService;
    private final AuditoriaService auditoria;

    public PagamentoService(PedidoService pedidoService, PagamentoRepository pagamentoRepository, GatewayPagamento gateway,
                            FidelidadeService fidelidadeService, AuditoriaService auditoria) {
        this.pedidoService = pedidoService;
        this.pagamentoRepository = pagamentoRepository;
        this.gateway = gateway;
        this.fidelidadeService = fidelidadeService;
        this.auditoria = auditoria;
    }

    // noRollbackFor: a tentativa recusada/com erro PRECISA ficar registrada mesmo lançando exceção.
    @Transactional(noRollbackFor = {PagamentoRecusadoException.class, GatewayIndisponivelException.class})
    public PagamentoResponse pagar(PagamentoRequest req, UsuarioLogado logado) {
        Pedido pedido = pedidoService.buscarEntidade(req.pedidoId()); // 404 se não existir
        pedidoService.verificarAcesso(pedido, logado);                 // 403 se não for dono/unidade

        if (pedido.getStatus() == StatusPedido.CANCELADO) {
            throw new RegraNegocioException("PEDIDO_CANCELADO", "Não é possível pagar um pedido cancelado.");
        }
        if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new RegraNegocioException("PEDIDO_JA_PAGO", "Este pedido já foi pago (status atual: " + pedido.getStatus() + ").");
        }

        SolicitacaoPagamento solicitacao = new SolicitacaoPagamento(
                "PEDIDO-" + pedido.getId(), pedido.getValorTotal(), req.metodo(), req.tokenPagamento());

        RespostaGateway resposta = chamarGatewayComRetentativa(solicitacao);

        if (resposta == null) {
            // Tolerância a falhas: registra a tentativa com ERRO e devolve 503; o cliente pode tentar de novo.
            Pagamento erro = pagamentoRepository.save(new Pagamento(pedido, req.metodo(), StatusPagamento.ERRO, null,
                    "Gateway indisponível após " + MAX_TENTATIVAS + " tentativas"));
            auditoria.registrar(logado.id(), "PAGAMENTO_ERRO", "PEDIDO", pedido.getId(), "pagamento=" + erro.getId());
            throw new GatewayIndisponivelException("Serviço de pagamento indisponível no momento. Tente novamente em instantes.", erro.getId());
        }

        if (!resposta.aprovado()) {
            Pagamento recusado = pagamentoRepository.save(new Pagamento(pedido, req.metodo(), StatusPagamento.RECUSADO,
                    resposta.codigoTransacao(), resposta.mensagem()));
            auditoria.registrar(logado.id(), "PAGAMENTO_RECUSADO", "PEDIDO", pedido.getId(),
                    "pagamento=" + recusado.getId() + " motivo=" + resposta.mensagem());
            throw new PagamentoRecusadoException(resposta.mensagem(), recusado.getId(), pedido.getId());
        }

        Pagamento aprovado = pagamentoRepository.save(new Pagamento(pedido, req.metodo(), StatusPagamento.APROVADO,
                resposta.codigoTransacao(), resposta.mensagem()));
        pedido.alterarStatus(StatusPedido.PAGO); // a cozinha assume o pedido a partir daqui
        int pontos = fidelidadeService.acumularPorPedido(pedido);

        auditoria.registrar(logado.id(), "PAGAMENTO_APROVADO", "PEDIDO", pedido.getId(),
                "pagamento=" + aprovado.getId() + " metodo=" + req.metodo() + " valor=" + pedido.getValorTotal() + " pontos=" + pontos);
        return PagamentoResponse.de(aprovado, pontos);
    }

    @Transactional(readOnly = true)
    public List<PagamentoResponse> listarPorPedido(Long pedidoId, UsuarioLogado logado) {
        Pedido pedido = pedidoService.buscarEntidade(pedidoId);
        pedidoService.verificarAcesso(pedido, logado);
        return pagamentoRepository.findByPedidoIdOrderByCriadoEmAsc(pedidoId).stream()
                .map(p -> PagamentoResponse.de(p, null)).toList();
    }

    /** Retentativa simples com espera crescente (200ms, 400ms). Retorna null se todas falharem. */
    private RespostaGateway chamarGatewayComRetentativa(SolicitacaoPagamento solicitacao) {
        for (int tentativa = 1; tentativa <= MAX_TENTATIVAS; tentativa++) {
            try {
                return gateway.processar(solicitacao);
            } catch (GatewayIndisponivelException e) {
                log.warn("Gateway indisponível (tentativa {}/{}) para {}", tentativa, MAX_TENTATIVAS, solicitacao.referencia());
                if (tentativa < MAX_TENTATIVAS) {
                    try {
                        Thread.sleep(200L * tentativa);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            }
        }
        return null;
    }
}
