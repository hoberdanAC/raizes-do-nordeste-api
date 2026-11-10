package br.com.raizesdonordeste.application.service;

import br.com.raizesdonordeste.application.dto.Pagina;
import br.com.raizesdonordeste.application.dto.Paginacao;
import br.com.raizesdonordeste.application.dto.PedidoDtos.*;
import br.com.raizesdonordeste.application.dto.UsuarioLogado;
import br.com.raizesdonordeste.domain.enums.*;
import br.com.raizesdonordeste.domain.exception.*;
import br.com.raizesdonordeste.domain.model.*;
import br.com.raizesdonordeste.infrastructure.repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/**
 * Casos de uso do Pedido (fluxo crítico): criação com validação de estoque,
 * listagem com filtros, consulta, mudança de status e cancelamento, com auditoria.
 */
@Service
public class PedidoService {

    /** Status que cada perfil pode definir manualmente via PATCH /pedidos/{id}/status. */
    private static final Map<Perfil, Set<StatusPedido>> STATUS_POR_PERFIL = Map.of(
            Perfil.COZINHA, EnumSet.of(StatusPedido.EM_PREPARACAO, StatusPedido.PRONTO),
            Perfil.ATENDENTE, EnumSet.of(StatusPedido.ENTREGUE),
            Perfil.GERENTE, EnumSet.of(StatusPedido.EM_PREPARACAO, StatusPedido.PRONTO, StatusPedido.ENTREGUE),
            Perfil.ADMIN, EnumSet.of(StatusPedido.EM_PREPARACAO, StatusPedido.PRONTO, StatusPedido.ENTREGUE));

    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final UnidadeRepository unidadeRepository;
    private final ProdutoRepository produtoRepository;
    private final EstoqueRepository estoqueRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;
    private final PagamentoRepository pagamentoRepository;
    private final FidelidadeService fidelidadeService;
    private final AuditoriaService auditoria;

    public PedidoService(PedidoRepository pedidoRepository, UsuarioRepository usuarioRepository,
                         UnidadeRepository unidadeRepository, ProdutoRepository produtoRepository,
                         EstoqueRepository estoqueRepository, MovimentacaoEstoqueRepository movimentacaoRepository,
                         PagamentoRepository pagamentoRepository, FidelidadeService fidelidadeService,
                         AuditoriaService auditoria) {
        this.pedidoRepository = pedidoRepository;
        this.usuarioRepository = usuarioRepository;
        this.unidadeRepository = unidadeRepository;
        this.produtoRepository = produtoRepository;
        this.estoqueRepository = estoqueRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.fidelidadeService = fidelidadeService;
        this.auditoria = auditoria;
    }

    /**
     * Cria o pedido:
     *  1. idempotência (mesma Idempotency-Key devolve o pedido já criado);
     *  2. valida canal x perfil;
     *  3. valida unidade, produtos e estoque (404/409);
     *  4. calcula total com preço do catálogo;
     *  5. RESERVA o estoque (baixa já na criação, devolvida se o pedido for cancelado);
     *  6. registra auditoria.
     * Tudo em uma única transação: se algo falhar, nada é gravado.
     */
    @Transactional
    public ResultadoCriacaoPedido criar(CriarPedidoRequest req, String chaveIdempotencia, UsuarioLogado logado) {
        String chave = (chaveIdempotencia == null || chaveIdempotencia.isBlank()) ? null : chaveIdempotencia.trim();
        if (chave != null) {
            Optional<Pedido> existente = pedidoRepository.findByCriadoPorIdAndChaveIdempotencia(logado.id(), chave);
            if (existente.isPresent()) {
                return new ResultadoCriacaoPedido(PedidoResponse.de(existente.get()), true);
            }
        }

        Usuario criador = usuarioRepository.findById(logado.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("USUARIO_NAO_ENCONTRADO", "Usuário não encontrado."));
        Usuario cliente = definirClienteEValidarCanal(req, logado, criador);

        Unidade unidade = unidadeRepository.findById(req.unidadeId())
                .filter(Unidade::isAtiva)
                .orElseThrow(() -> new RecursoNaoEncontradoException("UNIDADE_NAO_ENCONTRADA",
                        "Unidade " + req.unidadeId() + " não encontrada ou inativa."));

        // Agrupa itens repetidos (ex.: produto 1 enviado duas vezes) somando as quantidades.
        Map<Long, Integer> quantidades = new LinkedHashMap<>();
        Map<Long, Integer> indiceOriginal = new HashMap<>();
        for (int i = 0; i < req.itens().size(); i++) {
            ItemPedidoRequest item = req.itens().get(i);
            quantidades.merge(item.produtoId(), item.quantidade(), Integer::sum);
            indiceOriginal.putIfAbsent(item.produtoId(), i);
        }

        // Valida TODOS os itens antes de gravar, para devolver a lista completa de problemas.
        List<Violacao> faltas = new ArrayList<>();
        Map<Long, Estoque> estoques = new LinkedHashMap<>();
        Map<Long, Produto> produtos = new HashMap<>();
        for (var entrada : quantidades.entrySet()) {
            Long produtoId = entrada.getKey();
            Produto produto = produtoRepository.findById(produtoId)
                    .filter(Produto::isAtivo)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("PRODUTO_NAO_ENCONTRADO",
                            "Produto " + produtoId + " não encontrado."));
            Estoque estoque = estoqueRepository.buscarParaAtualizacao(unidade.getId(), produtoId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("PRODUTO_INDISPONIVEL_NA_UNIDADE",
                            "Produto '" + produto.getNome() + "' não faz parte do cardápio desta unidade."));
            if (!estoque.possui(entrada.getValue())) {
                faltas.add(new Violacao("itens[" + indiceOriginal.get(produtoId) + "].quantidade",
                        produto.getNome() + " - solicitado: " + entrada.getValue() + ", disponível: " + estoque.getQuantidade()));
            }
            produtos.put(produtoId, produto);
            estoques.put(produtoId, estoque);
        }
        if (!faltas.isEmpty()) {
            throw new RegraNegocioException("ESTOQUE_INSUFICIENTE",
                    "Não há quantidade suficiente para um ou mais itens.", faltas);
        }

        Pedido pedido = new Pedido(cliente, criador, unidade, req.canalPedido(), chave);
        quantidades.forEach((produtoId, qtd) -> {
            pedido.adicionarItem(produtos.get(produtoId), qtd);
            estoques.get(produtoId).baixar(qtd); // reserva
        });
        pedidoRepository.save(pedido);

        estoques.forEach((produtoId, estoque) -> movimentacaoRepository.save(new MovimentacaoEstoque(estoque,
                TipoMovimentacao.SAIDA, quantidades.get(produtoId), "Reserva para pedido #" + pedido.getId(), logado.id())));

        auditoria.registrar(logado.id(), "PEDIDO_CRIADO", "PEDIDO", pedido.getId(),
                "canal=" + pedido.getCanalPedido() + " unidade=" + unidade.getId() + " total=" + pedido.getValorTotal());

        return new ResultadoCriacaoPedido(PedidoResponse.de(pedido), false);
    }

    /**
     * Regras de multicanalidade x perfil:
     *  - CLIENTE: APP, TOTEM, WEB ou PICKUP (BALCAO é exclusivo do atendente). Cliente = ele mesmo.
     *  - ATENDENTE: somente BALCAO, na própria unidade; pode identificar um cliente (opcional).
     */
    private Usuario definirClienteEValidarCanal(CriarPedidoRequest req, UsuarioLogado logado, Usuario criador) {
        if (logado.is(Perfil.CLIENTE)) {
            if (req.canalPedido() == CanalPedido.BALCAO) {
                throw new DadosInvalidosException("CANAL_INVALIDO_PARA_PERFIL",
                        "Pedidos de BALCAO são registrados pelo atendente.",
                        List.of(new Violacao("canalPedido", "CLIENTE pode usar APP, TOTEM, WEB ou PICKUP")));
            }
            return criador;
        }
        // ATENDENTE
        if (req.canalPedido() != CanalPedido.BALCAO) {
            throw new DadosInvalidosException("CANAL_INVALIDO_PARA_PERFIL", "Atendente registra apenas pedidos de BALCAO.",
                    List.of(new Violacao("canalPedido", "ATENDENTE deve usar BALCAO")));
        }
        if (!req.unidadeId().equals(logado.unidadeId())) {
            throw new AcessoNegadoException("Atendente só pode registrar pedidos na própria unidade.");
        }
        if (req.clienteId() == null) {
            return null; // venda de balcão sem identificação do cliente
        }
        return usuarioRepository.findById(req.clienteId())
                .filter(u -> u.getPerfil() == Perfil.CLIENTE && u.isAtivo())
                .orElseThrow(() -> new RecursoNaoEncontradoException("CLIENTE_NAO_ENCONTRADO", "Cliente informado não encontrado."));
    }

    /** Listagem com filtros (?canalPedido=TOTEM&status=PAGO&unidadeId=1&page=1&limit=10). */
    @Transactional(readOnly = true)
    public Pagina<PedidoResumoResponse> listar(CanalPedido canal, StatusPedido status, Long unidadeId,
                                               Integer page, Integer limit, UsuarioLogado logado) {
        Long filtroUnidade = unidadeId;
        Long filtroCliente = null;
        if (logado.is(Perfil.CLIENTE)) {
            filtroCliente = logado.id();          // cliente só vê os próprios pedidos
        } else if (logado.isFuncionarioDeUnidade()) {
            filtroUnidade = logado.unidadeId();   // funcionário só vê a própria unidade
        }
        Specification<Pedido> spec = Specification.where(PedidoSpecs.canal(canal))
                .and(PedidoSpecs.status(status))
                .and(PedidoSpecs.unidade(filtroUnidade))
                .and(PedidoSpecs.cliente(filtroCliente));
        var pageable = Paginacao.de(page, limit, Sort.by(Sort.Direction.DESC, "criadoEm"));
        return Pagina.de(pedidoRepository.findAll(spec, pageable), PedidoResumoResponse::de);
    }

    @Transactional(readOnly = true)
    public PedidoResponse buscar(Long id, UsuarioLogado logado) {
        Pedido pedido = buscarEntidade(id);
        verificarAcesso(pedido, logado);
        return PedidoResponse.de(pedido);
    }

    /** Atualização de status (cozinha -> pronto -> entregue). Pagamento e cancelamento têm rotas próprias. */
    @Transactional
    public PedidoResponse atualizarStatus(Long id, StatusPedido novo, UsuarioLogado logado) {
        Pedido pedido = buscarEntidade(id);
        verificarAcesso(pedido, logado);

        if (novo == StatusPedido.PAGO || novo == StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new DadosInvalidosException("STATUS_NAO_PERMITIDO", "O status " + novo + " é definido apenas pelo fluxo de pagamento.",
                    List.of(new Violacao("status", "use POST /pagamentos")));
        }
        if (novo == StatusPedido.CANCELADO) {
            throw new DadosInvalidosException("STATUS_NAO_PERMITIDO", "Use POST /pedidos/{id}/cancelamento para cancelar.",
                    List.of(new Violacao("status", "use a rota de cancelamento")));
        }
        if (!STATUS_POR_PERFIL.getOrDefault(logado.perfil(), Set.of()).contains(novo)) {
            throw new AcessoNegadoException("O perfil " + logado.perfil() + " não pode definir o status " + novo + ".");
        }

        StatusPedido anterior = pedido.getStatus();
        pedido.alterarStatus(novo); // domínio valida a transição (409 se inválida)
        auditoria.registrar(logado.id(), "PEDIDO_STATUS_ALTERADO", "PEDIDO", pedido.getId(), anterior + " -> " + novo);
        return PedidoResponse.de(pedido);
    }

    /**
     * Cancelamento:
     *  - CLIENTE cancela apenas pedido ainda não pago;
     *  - ATENDENTE/GERENTE/ADMIN cancelam também pedido PAGO (gera estorno mock + estorno de pontos);
     *  - estoque reservado é devolvido.
     */
    @Transactional
    public PedidoResponse cancelar(Long id, String motivo, UsuarioLogado logado) {
        Pedido pedido = buscarEntidade(id);
        verificarAcesso(pedido, logado);

        StatusPedido anterior = pedido.getStatus();
        if (logado.is(Perfil.CLIENTE) && anterior != StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new RegraNegocioException("CANCELAMENTO_NAO_PERMITIDO",
                    "Pedido já pago só pode ser cancelado pelo atendente ou gerente.");
        }
        pedido.alterarStatus(StatusPedido.CANCELADO); // 409 se já estiver em preparo/pronto/entregue

        // Devolve o estoque reservado.
        for (ItemPedido item : pedido.getItens()) {
            estoqueRepository.buscarParaAtualizacao(pedido.getUnidade().getId(), item.getProduto().getId()).ifPresent(estoque -> {
                estoque.repor(item.getQuantidade());
                movimentacaoRepository.save(new MovimentacaoEstoque(estoque, TipoMovimentacao.ENTRADA, item.getQuantidade(),
                        "Estorno do pedido cancelado #" + pedido.getId(), logado.id()));
            });
        }

        // Se estava pago: estorna pagamento (mock) e pontos de fidelidade.
        if (anterior == StatusPedido.PAGO) {
            pagamentoRepository.findFirstByPedidoIdAndStatus(pedido.getId(), StatusPagamento.APROVADO).ifPresent(Pagamento::estornar);
            fidelidadeService.estornarPorPedido(pedido);
        }

        auditoria.registrar(logado.id(), "PEDIDO_CANCELADO", "PEDIDO", pedido.getId(),
                "statusAnterior=" + anterior + " motivo=" + (motivo == null ? "não informado" : motivo));
        return PedidoResponse.de(pedido);
    }

    /** Usado também pelo PagamentoService. */
    public Pedido buscarEntidade(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("PEDIDO_NAO_ENCONTRADO", "Pedido " + id + " não encontrado."));
    }

    /** Controle de acesso por dono/unidade (autorização em nível de registro, além do perfil). */
    public void verificarAcesso(Pedido pedido, UsuarioLogado logado) {
        if (logado.is(Perfil.ADMIN)) return;
        if (logado.is(Perfil.CLIENTE)) {
            if (!pedido.pertenceAoCliente(logado.id())) {
                throw new AcessoNegadoException("Este pedido pertence a outro cliente.");
            }
            return;
        }
        if (!pedido.getUnidade().getId().equals(logado.unidadeId())) {
            throw new AcessoNegadoException("Este pedido pertence a outra unidade.");
        }
    }
}
