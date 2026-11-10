package br.com.raizesdonordeste.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Produto do catálogo da rede. O PREÇO OFICIAL fica aqui.
 * O preço nunca é aceito do cliente no pedido (evita fraude como comprar por R$ 0,01):
 * o valor sempre vem do banco.
 */
@Entity
@Table(name = "produtos")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 255)
    private String descricao;

    @Column(nullable = false, length = 50)
    private String categoria;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    protected Produto() { }

    public Produto(String nome, String descricao, String categoria, BigDecimal preco) {
        this.nome = nome;
        this.descricao = descricao;
        this.categoria = categoria.toUpperCase();
        this.preco = preco;
        this.criadoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public String getCategoria() { return categoria; }
    public BigDecimal getPreco() { return preco; }
    public boolean isAtivo() { return ativo; }
}
