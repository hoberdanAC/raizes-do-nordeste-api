package br.com.raizesdonordeste.domain.model;

import br.com.raizesdonordeste.domain.exception.RegraNegocioException;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Saldo de um produto em uma unidade. A regra "não vender sem estoque" mora aqui,
 * no domínio, e não no controller.
 */
@Entity
@Table(name = "estoques")
public class Estoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id", nullable = false)
    private Unidade unidade;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false)
    private int quantidade;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    protected Estoque() { }

    public Estoque(Unidade unidade, Produto produto) {
        this.unidade = unidade;
        this.produto = produto;
        this.quantidade = 0;
        this.atualizadoEm = LocalDateTime.now();
    }

    public boolean possui(int qtd) {
        return quantidade >= qtd;
    }

    /** Saída de estoque. Lança 409 se não houver saldo suficiente. */
    public void baixar(int qtd) {
        if (!possui(qtd)) {
            throw new RegraNegocioException("ESTOQUE_INSUFICIENTE",
                    "Estoque insuficiente para '" + produto.getNome() + "'. Disponível: " + quantidade);
        }
        this.quantidade -= qtd;
        this.atualizadoEm = LocalDateTime.now();
    }

    /** Entrada de estoque (compra de insumos ou estorno de pedido cancelado). */
    public void repor(int qtd) {
        this.quantidade += qtd;
        this.atualizadoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Unidade getUnidade() { return unidade; }
    public Produto getProduto() { return produto; }
    public int getQuantidade() { return quantidade; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
}
