package br.com.raizesdonordeste.domain.model;

import br.com.raizesdonordeste.domain.enums.CanalPedido;
import br.com.raizesdonordeste.domain.enums.StatusPedido;
import br.com.raizesdonordeste.domain.exception.RegraNegocioException;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Agregado Pedido. Concentra as regras do fluxo crítico:
 *  - canalPedido obrigatório (multicanalidade);
 *  - total calculado a partir dos preços do catálogo;
 *  - transições de status controladas (ver StatusPedido).
 */
@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Cliente dono do pedido (pode ser nulo em venda de balcão sem identificação). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Usuario cliente;

    /** Quem registrou o pedido (o próprio cliente ou o atendente). Rastreabilidade. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criado_por_id", nullable = false)
    private Usuario criadoPor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id", nullable = false)
    private Unidade unidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal_pedido", nullable = false, length = 10)
    private CanalPedido canalPedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private StatusPedido status;

    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal = BigDecimal.ZERO;

    /** Chave enviada no header Idempotency-Key: evita pedido duplicado se o app reenviar. */
    @Column(name = "chave_idempotencia", length = 64)
    private String chaveIdempotencia;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPedido> itens = new ArrayList<>();

    protected Pedido() { }

    public Pedido(Usuario cliente, Usuario criadoPor, Unidade unidade, CanalPedido canalPedido, String chaveIdempotencia) {
        if (canalPedido == null) {
            // Reforço no domínio: mesmo que alguém pule a validação da API, o pedido não nasce sem canal.
            throw new RegraNegocioException("CANAL_OBRIGATORIO", "O campo canalPedido é obrigatório.");
        }
        this.cliente = cliente;
        this.criadoPor = criadoPor;
        this.unidade = unidade;
        this.canalPedido = canalPedido;
        this.chaveIdempotencia = chaveIdempotencia;
        this.status = StatusPedido.AGUARDANDO_PAGAMENTO; // status inicial do fluxo
        this.criadoEm = LocalDateTime.now();
        this.atualizadoEm = this.criadoEm;
    }

    /** Adiciona item e recalcula o total (soma de quantidade x preço unitário). */
    public void adicionarItem(Produto produto, int quantidade) {
        ItemPedido item = new ItemPedido(this, produto, quantidade);
        itens.add(item);
        valorTotal = valorTotal.add(item.getSubtotal());
    }

    /** Altera o status respeitando a máquina de estados. Transição inválida = 409. */
    public void alterarStatus(StatusPedido novo) {
        if (!status.podeIrPara(novo)) {
            throw new RegraNegocioException("TRANSICAO_STATUS_INVALIDA",
                    "Não é possível mudar o pedido de " + status + " para " + novo
                            + ". Permitidos: " + status.proximosPermitidos());
        }
        this.status = novo;
        this.atualizadoEm = LocalDateTime.now();
    }

    public boolean pertenceAoCliente(Long usuarioId) {
        return cliente != null && cliente.getId().equals(usuarioId);
    }

    public Long getId() { return id; }
    public Usuario getCliente() { return cliente; }
    public Usuario getCriadoPor() { return criadoPor; }
    public Unidade getUnidade() { return unidade; }
    public CanalPedido getCanalPedido() { return canalPedido; }
    public StatusPedido getStatus() { return status; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public String getChaveIdempotencia() { return chaveIdempotencia; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public List<ItemPedido> getItens() { return itens; }
}
