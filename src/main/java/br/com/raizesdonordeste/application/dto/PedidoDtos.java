package br.com.raizesdonordeste.application.dto;

import br.com.raizesdonordeste.domain.enums.CanalPedido;
import br.com.raizesdonordeste.domain.enums.MetodoPagamento;
import br.com.raizesdonordeste.domain.enums.StatusPagamento;
import br.com.raizesdonordeste.domain.enums.StatusPedido;
import br.com.raizesdonordeste.domain.model.ItemPedido;
import br.com.raizesdonordeste.domain.model.Pagamento;
import br.com.raizesdonordeste.domain.model.Pedido;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Contratos do fluxo crítico: Pedido -> Pagamento mock -> Status. */
public final class PedidoDtos {

    private PedidoDtos() { }

    /**
     * Criação de pedido. Regras do contrato:
     *  - canalPedido é ENUM e @NotNull (ausente = 422, inválido = 400);
     *  - não recebe preço (vem do catálogo) nem clienteId (o cliente vem do token JWT);
     *  - recebe unidadeId (estoque é por unidade).
     */
    public record CriarPedidoRequest(
            @Schema(example = "APP", description = "APP, TOTEM, BALCAO, PICKUP ou WEB")
            @NotNull(message = "é obrigatório") CanalPedido canalPedido,
            @Schema(example = "1") @NotNull Long unidadeId,
            @Schema(description = "Usado apenas pelo ATENDENTE para identificar o cliente no balcão (opcional)")
            Long clienteId,
            @NotEmpty(message = "deve conter ao menos 1 item") @Size(max = 30) @Valid List<ItemPedidoRequest> itens) { }

    public record ItemPedidoRequest(
            @Schema(example = "1") @NotNull Long produtoId,
            @Schema(example = "2") @NotNull @Positive(message = "deve ser maior que zero") @Max(50) Integer quantidade) { }

    public record ItemPedidoResponse(Long produtoId, String nome, int quantidade, BigDecimal precoUnitario, BigDecimal subtotal) {
        public static ItemPedidoResponse de(ItemPedido i) {
            return new ItemPedidoResponse(i.getProduto().getId(), i.getProduto().getNome(), i.getQuantidade(),
                    i.getPrecoUnitario(), i.getSubtotal());
        }
    }

    public record PedidoResponse(Long pedidoId, CanalPedido canalPedido, StatusPedido status, Long unidadeId,
                                 Long clienteId, BigDecimal total, List<ItemPedidoResponse> itens,
                                 LocalDateTime criadoEm, LocalDateTime atualizadoEm) {
        public static PedidoResponse de(Pedido p) {
            return new PedidoResponse(p.getId(), p.getCanalPedido(), p.getStatus(), p.getUnidade().getId(),
                    p.getCliente() != null ? p.getCliente().getId() : null, p.getValorTotal(),
                    p.getItens().stream().map(ItemPedidoResponse::de).toList(), p.getCriadoEm(), p.getAtualizadoEm());
        }
    }

    /** Versão resumida usada na listagem (sem itens, mais leve em horário de pico). */
    public record PedidoResumoResponse(Long pedidoId, CanalPedido canalPedido, StatusPedido status, Long unidadeId,
                                       BigDecimal total, LocalDateTime criadoEm) {
        public static PedidoResumoResponse de(Pedido p) {
            return new PedidoResumoResponse(p.getId(), p.getCanalPedido(), p.getStatus(), p.getUnidade().getId(),
                    p.getValorTotal(), p.getCriadoEm());
        }
    }

    /** Resultado interno: "reaproveitado" = true quando a Idempotency-Key já tinha sido usada. */
    public record ResultadoCriacaoPedido(PedidoResponse pedido, boolean reaproveitado) { }

    public record AtualizarStatusRequest(
            @Schema(example = "EM_PREPARACAO") @NotNull StatusPedido status) { }

    public record CancelarPedidoRequest(
            @Schema(example = "Cliente desistiu") @Size(max = 200) String motivo) { }

    // ---------- Pagamento ----------
    /** Solicitação de pagamento, com token opcional para simular cenários do gateway mock. */
    public record PagamentoRequest(
            @Schema(example = "1") @NotNull Long pedidoId,
            @Schema(example = "PIX") @NotNull MetodoPagamento metodo,
            @Schema(example = "tok_aprovado",
                    description = "Mock: 'tok_recusado' = gateway nega; 'tok_falha' = gateway fora do ar; qualquer outro = aprova")
            @Size(max = 60) String tokenPagamento) { }

    public record PagamentoResponse(Long pagamentoId, Long pedidoId, MetodoPagamento metodo, StatusPagamento status,
                                    BigDecimal valor, String codigoTransacao, String mensagem,
                                    StatusPedido statusPedido, Integer pontosFidelidadeGanhos, LocalDateTime criadoEm) {
        public static PagamentoResponse de(Pagamento pg, Integer pontos) {
            return new PagamentoResponse(pg.getId(), pg.getPedido().getId(), pg.getMetodo(), pg.getStatus(), pg.getValor(),
                    pg.getCodigoTransacao(), pg.getMensagemGateway(), pg.getPedido().getStatus(), pontos, pg.getCriadoEm());
        }
    }
}
