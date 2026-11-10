package br.com.raizesdonordeste.application.service;

import br.com.raizesdonordeste.application.dto.CatalogoDtos.*;
import br.com.raizesdonordeste.application.dto.Pagina;
import br.com.raizesdonordeste.application.dto.Paginacao;
import br.com.raizesdonordeste.application.dto.UsuarioLogado;
import br.com.raizesdonordeste.domain.model.Produto;
import br.com.raizesdonordeste.domain.model.Unidade;
import br.com.raizesdonordeste.domain.exception.RecursoNaoEncontradoException;
import br.com.raizesdonordeste.infrastructure.repository.EstoqueRepository;
import br.com.raizesdonordeste.infrastructure.repository.ProdutoRepository;
import br.com.raizesdonordeste.infrastructure.repository.UnidadeRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Casos de uso de unidades, produtos e cardápio por unidade. */
@Service
public class CatalogoService {

    private final UnidadeRepository unidadeRepository;
    private final ProdutoRepository produtoRepository;
    private final EstoqueRepository estoqueRepository;
    private final AuditoriaService auditoria;

    public CatalogoService(UnidadeRepository unidadeRepository, ProdutoRepository produtoRepository,
                           EstoqueRepository estoqueRepository, AuditoriaService auditoria) {
        this.unidadeRepository = unidadeRepository;
        this.produtoRepository = produtoRepository;
        this.estoqueRepository = estoqueRepository;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public Pagina<UnidadeResponse> listarUnidades(Integer page, Integer limit) {
        return Pagina.de(unidadeRepository.findAll(Paginacao.de(page, limit, Sort.by("id"))), UnidadeResponse::de);
    }

    @Transactional
    public UnidadeResponse criarUnidade(UnidadeRequest req, UsuarioLogado admin) {
        Unidade unidade = unidadeRepository.save(new Unidade(req.nome().trim(), req.cidade().trim(), req.uf()));
        auditoria.registrar(admin.id(), "UNIDADE_CRIADA", "UNIDADE", unidade.getId(), unidade.getNome());
        return UnidadeResponse.de(unidade);
    }

    /** Cardápio da unidade = produtos ativos que possuem linha de estoque naquela unidade. */
    @Transactional(readOnly = true)
    public CardapioResponse cardapio(Long unidadeId) {
        Unidade unidade = unidadeRepository.findById(unidadeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("UNIDADE_NAO_ENCONTRADA", "Unidade " + unidadeId + " não encontrada."));
        var itens = estoqueRepository.listarPorUnidade(unidadeId).stream()
                .filter(e -> e.getProduto().isAtivo())
                .map(ItemCardapioResponse::de)
                .toList();
        return new CardapioResponse(unidade.getId(), unidade.getNome(), itens);
    }

    @Transactional(readOnly = true)
    public Pagina<ProdutoResponse> listarProdutos(Integer page, Integer limit) {
        return Pagina.de(produtoRepository.findByAtivoTrue(Paginacao.de(page, limit, Sort.by("nome"))), ProdutoResponse::de);
    }

    @Transactional(readOnly = true)
    public ProdutoResponse buscarProduto(Long id) {
        return ProdutoResponse.de(produtoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("PRODUTO_NAO_ENCONTRADO", "Produto " + id + " não encontrado.")));
    }

    @Transactional
    public ProdutoResponse criarProduto(ProdutoRequest req, UsuarioLogado usuario) {
        Produto produto = produtoRepository.save(new Produto(req.nome().trim(), req.descricao(), req.categoria().trim(), req.preco()));
        auditoria.registrar(usuario.id(), "PRODUTO_CRIADO", "PRODUTO", produto.getId(), produto.getNome() + " R$ " + produto.getPreco());
        return ProdutoResponse.de(produto);
    }
}
