package br.com.raizesdonordeste.domain.model;

import br.com.raizesdonordeste.domain.enums.TipoMovimentacao;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Registro histórico de cada entrada/saída de estoque (quem, quanto, por quê). */
@Entity
@Table(name = "movimentacoes_estoque")
public class MovimentacaoEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estoque_id", nullable = false)
    private Estoque estoque;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimentacao tipo;

    @Column(nullable = false)
    private int quantidade;

    @Column(nullable = false, length = 255)
    private String motivo;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    protected MovimentacaoEstoque() { }

    public MovimentacaoEstoque(Estoque estoque, TipoMovimentacao tipo, int quantidade, String motivo, Long usuarioId) {
        this.estoque = estoque;
        this.tipo = tipo;
        this.quantidade = quantidade;
        this.motivo = motivo;
        this.usuarioId = usuarioId;
        this.criadoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Estoque getEstoque() { return estoque; }
    public TipoMovimentacao getTipo() { return tipo; }
    public int getQuantidade() { return quantidade; }
    public String getMotivo() { return motivo; }
    public Long getUsuarioId() { return usuarioId; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
