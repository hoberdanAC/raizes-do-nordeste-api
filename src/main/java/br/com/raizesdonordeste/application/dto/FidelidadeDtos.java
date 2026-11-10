package br.com.raizesdonordeste.application.dto;

import br.com.raizesdonordeste.domain.enums.TipoTransacaoFidelidade;
import br.com.raizesdonordeste.domain.model.LogAuditoria;
import br.com.raizesdonordeste.domain.model.TransacaoFidelidade;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Contratos do programa de fidelidade e da auditoria. */
public final class FidelidadeDtos {

    private FidelidadeDtos() { }

    public record ConsentimentoRequest(
            @Schema(example = "true", description = "true = aceita participar; false = revoga o consentimento")
            @NotNull Boolean aceito) { }

    public record SaldoResponse(Long usuarioId, long saldoPontos, boolean consentimentoAtivo,
                                LocalDateTime dataConsentimento, String regra) { }

    public record ResgateRequest(
            @Schema(example = "100", description = "Múltiplos de 100 pontos") @NotNull @Min(100) Integer pontos) { }

    public record ResgateResponse(int pontosResgatados, long saldoRestante, BigDecimal valorDesconto, String cupom) { }

    public record TransacaoResponse(Long id, TipoTransacaoFidelidade tipo, int pontos, String descricao,
                                    Long pedidoId, LocalDateTime criadoEm) {
        public static TransacaoResponse de(TransacaoFidelidade t) {
            return new TransacaoResponse(t.getId(), t.getTipo(), t.getPontos(), t.getDescricao(),
                    t.getPedido() != null ? t.getPedido().getId() : null, t.getCriadoEm());
        }
    }

    public record LogAuditoriaResponse(Long id, Long usuarioId, String acao, String entidade, Long entidadeId,
                                       String detalhes, LocalDateTime criadoEm) {
        public static LogAuditoriaResponse de(LogAuditoria l) {
            return new LogAuditoriaResponse(l.getId(), l.getUsuarioId(), l.getAcao(), l.getEntidade(),
                    l.getEntidadeId(), l.getDetalhes(), l.getCriadoEm());
        }
    }
}
