package br.com.raizesdonordeste.application.dto;

import br.com.raizesdonordeste.domain.enums.TipoMovimentacao;
import br.com.raizesdonordeste.domain.model.Estoque;
import br.com.raizesdonordeste.domain.model.Produto;
import br.com.raizesdonordeste.domain.model.Unidade;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Contratos de unidades, produtos, cardápio e estoque. */
public final class CatalogoDtos {

    private CatalogoDtos() { }

    // ---------- Unidades ----------
    public record UnidadeRequest(
            @Schema(example = "Raízes do Nordeste - Ponta Verde") @NotBlank @Size(max = 120) String nome,
            @Schema(example = "Maceió") @NotBlank @Size(max = 80) String cidade,
            @Schema(example = "AL") @NotBlank @Pattern(regexp = "[A-Za-z]{2}", message = "deve ter 2 letras") String uf) { }

    public record UnidadeResponse(Long id, String nome, String cidade, String uf, boolean ativa) {
        public static UnidadeResponse de(Unidade u) {
            return new UnidadeResponse(u.getId(), u.getNome(), u.getCidade(), u.getUf(), u.isAtiva());
        }
    }

    /** Item do cardápio público: mostra só se está disponível, não a quantidade exata do estoque. */
    public record ItemCardapioResponse(Long produtoId, String nome, String descricao, String categoria,
                                       BigDecimal preco, boolean disponivel) {
        public static ItemCardapioResponse de(Estoque e) {
            Produto p = e.getProduto();
            return new ItemCardapioResponse(p.getId(), p.getNome(), p.getDescricao(), p.getCategoria(),
                    p.getPreco(), e.getQuantidade() > 0);
        }
    }

    public record CardapioResponse(Long unidadeId, String unidadeNome, java.util.List<ItemCardapioResponse> itens) { }

    // ---------- Produtos ----------
    public record ProdutoRequest(
            @Schema(example = "Escondidinho de charque") @NotBlank @Size(max = 120) String nome,
            @Schema(example = "Purê de macaxeira com charque") @Size(max = 255) String descricao,
            @Schema(example = "PRATO") @NotBlank @Size(max = 50) String categoria,
            @Schema(example = "32.90") @NotNull @DecimalMin(value = "0.01") @Digits(integer = 8, fraction = 2) BigDecimal preco) { }

    public record ProdutoResponse(Long id, String nome, String descricao, String categoria, BigDecimal preco, boolean ativo) {
        public static ProdutoResponse de(Produto p) {
            return new ProdutoResponse(p.getId(), p.getNome(), p.getDescricao(), p.getCategoria(), p.getPreco(), p.isAtivo());
        }
    }

    // ---------- Estoque ----------
    public record MovimentacaoEstoqueRequest(
            @Schema(example = "1") @NotNull Long unidadeId,
            @Schema(example = "3") @NotNull Long produtoId,
            @Schema(example = "ENTRADA") @NotNull TipoMovimentacao tipo,
            @Schema(example = "10") @NotNull @Positive @Max(10000) Integer quantidade,
            @Schema(example = "Recebimento de fornecedor") @NotBlank @Size(max = 255) String motivo) { }

    public record EstoqueResponse(Long unidadeId, Long produtoId, String produtoNome, int quantidade, LocalDateTime atualizadoEm) {
        public static EstoqueResponse de(Estoque e) {
            return new EstoqueResponse(e.getUnidade().getId(), e.getProduto().getId(), e.getProduto().getNome(),
                    e.getQuantidade(), e.getAtualizadoEm());
        }
    }
}
