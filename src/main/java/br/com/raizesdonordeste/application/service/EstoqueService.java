package br.com.raizesdonordeste.application.service;

import br.com.raizesdonordeste.application.dto.CatalogoDtos.EstoqueResponse;
import br.com.raizesdonordeste.application.dto.CatalogoDtos.MovimentacaoEstoqueRequest;
import br.com.raizesdonordeste.application.dto.UsuarioLogado;
import br.com.raizesdonordeste.domain.enums.Perfil;
import br.com.raizesdonordeste.domain.enums.TipoMovimentacao;
import br.com.raizesdonordeste.domain.exception.AcessoNegadoException;
import br.com.raizesdonordeste.domain.exception.RecursoNaoEncontradoException;
import br.com.raizesdonordeste.domain.exception.RegraNegocioException;
import br.com.raizesdonordeste.domain.exception.Violacao;
import br.com.raizesdonordeste.domain.model.Estoque;
import br.com.raizesdonordeste.domain.model.MovimentacaoEstoque;
import br.com.raizesdonordeste.domain.model.Produto;
import br.com.raizesdonordeste.domain.model.Unidade;
import br.com.raizesdonordeste.infrastructure.repository.EstoqueRepository;
import br.com.raizesdonordeste.infrastructure.repository.MovimentacaoEstoqueRepository;
import br.com.raizesdonordeste.infrastructure.repository.ProdutoRepository;
import br.com.raizesdonordeste.infrastructure.repository.UnidadeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/** Casos de uso de estoque por unidade: consulta de saldo e entrada/saída manual. */
@Service
public class EstoqueService {

    private final EstoqueRepository estoqueRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;
    private final UnidadeRepository unidadeRepository;
    private final ProdutoRepository produtoRepository;
    private final AuditoriaService auditoria;

    public EstoqueService(EstoqueRepository estoqueRepository, MovimentacaoEstoqueRepository movimentacaoRepository,
                          UnidadeRepository unidadeRepository, ProdutoRepository produtoRepository, AuditoriaService auditoria) {
        this.estoqueRepository = estoqueRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.unidadeRepository = unidadeRepository;
        this.produtoRepository = produtoRepository;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<EstoqueResponse> saldoPorUnidade(Long unidadeId, UsuarioLogado usuario) {
        verificarEscopoUnidade(unidadeId, usuario);
        unidadeRepository.findById(unidadeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("UNIDADE_NAO_ENCONTRADA", "Unidade " + unidadeId + " não encontrada."));
        return estoqueRepository.listarPorUnidade(unidadeId).stream().map(EstoqueResponse::de).toList();
    }

    @Transactional
    public EstoqueResponse movimentar(MovimentacaoEstoqueRequest req, UsuarioLogado usuario) {
        verificarEscopoUnidade(req.unidadeId(), usuario);
        Unidade unidade = unidadeRepository.findById(req.unidadeId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("UNIDADE_NAO_ENCONTRADA", "Unidade não encontrada."));
        Produto produto = produtoRepository.findById(req.produtoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("PRODUTO_NAO_ENCONTRADO", "Produto não encontrado."));

        Estoque estoque = estoqueRepository.buscarParaAtualizacao(unidade.getId(), produto.getId()).orElse(null);

        if (req.tipo() == TipoMovimentacao.ENTRADA) {
            if (estoque == null) {
                // Primeira entrada do produto na unidade: passa a fazer parte do cardápio dela.
                estoque = estoqueRepository.save(new Estoque(unidade, produto));
            }
            estoque.repor(req.quantidade());
        } else {
            if (estoque == null) {
                throw new RecursoNaoEncontradoException("PRODUTO_INDISPONIVEL_NA_UNIDADE", "Produto não faz parte do estoque desta unidade.");
            }
            if (!estoque.possui(req.quantidade())) {
                throw new RegraNegocioException("ESTOQUE_INSUFICIENTE", "Saída maior que o saldo disponível.",
                        List.of(new Violacao("quantidade", "Disponível: " + estoque.getQuantidade())));
            }
            estoque.baixar(req.quantidade());
        }

        movimentacaoRepository.save(new MovimentacaoEstoque(estoque, req.tipo(), req.quantidade(), req.motivo(), usuario.id()));
        auditoria.registrar(usuario.id(), "ESTOQUE_" + req.tipo(), "ESTOQUE", estoque.getId(),
                "unidade=" + unidade.getId() + " produto=" + produto.getId() + " qtd=" + req.quantidade()
                        + " saldo=" + estoque.getQuantidade() + " motivo=" + req.motivo());
        return EstoqueResponse.de(estoque);
    }

    /** Gerente/atendente/cozinha só enxergam a própria unidade. ADMIN enxerga a rede toda. */
    private void verificarEscopoUnidade(Long unidadeId, UsuarioLogado usuario) {
        if (usuario.perfil() != Perfil.ADMIN && !unidadeId.equals(usuario.unidadeId())) {
            throw new AcessoNegadoException("Você só pode acessar o estoque da sua unidade.");
        }
    }
}
