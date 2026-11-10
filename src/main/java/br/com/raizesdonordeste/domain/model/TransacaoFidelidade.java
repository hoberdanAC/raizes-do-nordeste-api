package br.com.raizesdonordeste.domain.model;

import br.com.raizesdonordeste.domain.enums.TipoTransacaoFidelidade;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Lançamento no extrato de pontos. O saldo é a soma de todos os lançamentos do usuário. */
@Entity
@Table(name = "fidelidade_transacoes")
public class TransacaoFidelidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoTransacaoFidelidade tipo;

    @Column(nullable = false)
    private int pontos;

    @Column(nullable = false, length = 255)
    private String descricao;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    protected TransacaoFidelidade() { }

    public TransacaoFidelidade(Usuario usuario, Pedido pedido, TipoTransacaoFidelidade tipo, int pontos, String descricao) {
        this.usuario = usuario;
        this.pedido = pedido;
        this.tipo = tipo;
        this.pontos = pontos;
        this.descricao = descricao;
        this.criadoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Pedido getPedido() { return pedido; }
    public TipoTransacaoFidelidade getTipo() { return tipo; }
    public int getPontos() { return pontos; }
    public String getDescricao() { return descricao; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
