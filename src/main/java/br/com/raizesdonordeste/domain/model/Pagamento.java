package br.com.raizesdonordeste.domain.model;

import br.com.raizesdonordeste.domain.enums.MetodoPagamento;
import br.com.raizesdonordeste.domain.enums.StatusPagamento;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tentativa de pagamento. Fica em tabela própria (desacoplada do pedido): cada tentativa,
 * aprovada ou recusada, é registrada (histórico completo e rastreável).
 */
@Entity
@Table(name = "pagamentos")
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MetodoPagamento metodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private StatusPagamento status;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "codigo_transacao", length = 60)
    private String codigoTransacao;

    @Column(name = "mensagem_gateway", length = 255)
    private String mensagemGateway;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    protected Pagamento() { }

    public Pagamento(Pedido pedido, MetodoPagamento metodo, StatusPagamento status,
                     String codigoTransacao, String mensagemGateway) {
        this.pedido = pedido;
        this.metodo = metodo;
        this.status = status;
        this.valor = pedido.getValorTotal();
        this.codigoTransacao = codigoTransacao;
        this.mensagemGateway = mensagemGateway;
        this.criadoEm = LocalDateTime.now();
    }

    public void estornar() {
        this.status = StatusPagamento.ESTORNADO;
    }

    public Long getId() { return id; }
    public Pedido getPedido() { return pedido; }
    public MetodoPagamento getMetodo() { return metodo; }
    public StatusPagamento getStatus() { return status; }
    public BigDecimal getValor() { return valor; }
    public String getCodigoTransacao() { return codigoTransacao; }
    public String getMensagemGateway() { return mensagemGateway; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
