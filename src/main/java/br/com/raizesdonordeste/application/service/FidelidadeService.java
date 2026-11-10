package br.com.raizesdonordeste.application.service;

import br.com.raizesdonordeste.application.dto.FidelidadeDtos.*;
import br.com.raizesdonordeste.application.dto.Pagina;
import br.com.raizesdonordeste.application.dto.Paginacao;
import br.com.raizesdonordeste.application.dto.UsuarioLogado;
import br.com.raizesdonordeste.domain.enums.TipoTransacaoFidelidade;
import br.com.raizesdonordeste.domain.exception.DadosInvalidosException;
import br.com.raizesdonordeste.domain.exception.RecursoNaoEncontradoException;
import br.com.raizesdonordeste.domain.exception.RegraNegocioException;
import br.com.raizesdonordeste.domain.exception.Violacao;
import br.com.raizesdonordeste.domain.model.Pedido;
import br.com.raizesdonordeste.domain.model.TransacaoFidelidade;
import br.com.raizesdonordeste.domain.model.Usuario;
import br.com.raizesdonordeste.infrastructure.repository.TransacaoFidelidadeRepository;
import br.com.raizesdonordeste.infrastructure.repository.UsuarioRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * Programa de fidelidade (requisito obrigatório do roteiro).
 *
 * Regras:
 *  - só participa quem deu CONSENTIMENTO (LGPD - base legal art. 7º, I);
 *  - ganha 1 ponto por real inteiro de pedido PAGO;
 *  - resgate em múltiplos de 100 pontos; 100 pontos = R$ 5,00 de desconto (cupom);
 *  - pedido pago e depois cancelado tem os pontos estornados.
 */
@Service
public class FidelidadeService {

    public static final String REGRA = "1 ponto por R$ 1,00 pago. Resgate em múltiplos de 100 pontos (100 pontos = R$ 5,00).";
    private static final BigDecimal VALOR_POR_100_PONTOS = new BigDecimal("5.00");

    private final TransacaoFidelidadeRepository transacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoria;

    public FidelidadeService(TransacaoFidelidadeRepository transacaoRepository, UsuarioRepository usuarioRepository,
                             AuditoriaService auditoria) {
        this.transacaoRepository = transacaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoria = auditoria;
    }

    /** Chamado pelo PagamentoService quando o pagamento é aprovado. Retorna os pontos ganhos. */
    @Transactional
    public int acumularPorPedido(Pedido pedido) {
        Usuario cliente = pedido.getCliente();
        if (cliente == null || !cliente.isConsentimentoFidelidade()) {
            return 0; // sem consentimento, nenhum dado de fidelidade é gerado
        }
        int pontos = pedido.getValorTotal().setScale(0, RoundingMode.DOWN).intValue();
        if (pontos <= 0) return 0;
        transacaoRepository.save(new TransacaoFidelidade(cliente, pedido, TipoTransacaoFidelidade.ACUMULO, pontos,
                "Pontos do pedido #" + pedido.getId()));
        return pontos;
    }

    /** Chamado no cancelamento de pedido já pago: devolve os pontos ganhos (sem deixar saldo negativo). */
    @Transactional
    public void estornarPorPedido(Pedido pedido) {
        transacaoRepository.findFirstByPedidoIdAndTipo(pedido.getId(), TipoTransacaoFidelidade.ACUMULO).ifPresent(acumulo -> {
            Usuario cliente = pedido.getCliente();
            long saldo = transacaoRepository.calcularSaldo(cliente.getId());
            int estorno = (int) Math.min(acumulo.getPontos(), Math.max(saldo, 0));
            if (estorno > 0) {
                transacaoRepository.save(new TransacaoFidelidade(cliente, pedido, TipoTransacaoFidelidade.ESTORNO, -estorno,
                        "Estorno: pedido #" + pedido.getId() + " cancelado"));
            }
        });
    }

    @Transactional
    public SaldoResponse registrarConsentimento(UsuarioLogado logado, boolean aceito) {
        Usuario usuario = buscarUsuario(logado.id());
        usuario.definirConsentimentoFidelidade(aceito);
        auditoria.registrar(logado.id(), aceito ? "CONSENTIMENTO_CONCEDIDO" : "CONSENTIMENTO_REVOGADO",
                "USUARIO", logado.id(), "finalidade=programa de fidelidade");
        return montarSaldo(usuario);
    }

    @Transactional(readOnly = true)
    public SaldoResponse saldo(UsuarioLogado logado) {
        return montarSaldo(buscarUsuario(logado.id()));
    }

    @Transactional(readOnly = true)
    public Pagina<TransacaoResponse> historico(UsuarioLogado logado, Integer page, Integer limit) {
        var pageable = Paginacao.de(page, limit, Sort.unsorted());
        return Pagina.de(transacaoRepository.findByUsuarioIdOrderByCriadoEmDesc(logado.id(), pageable), TransacaoResponse::de);
    }

    @Transactional
    public ResgateResponse resgatar(UsuarioLogado logado, int pontos) {
        Usuario usuario = buscarUsuario(logado.id());
        if (!usuario.isConsentimentoFidelidade()) {
            throw new RegraNegocioException("CONSENTIMENTO_NECESSARIO",
                    "É necessário aceitar o programa de fidelidade (PUT /fidelidade/consentimento).");
        }
        if (pontos % 100 != 0) {
            throw new DadosInvalidosException("VALIDACAO_FALHOU", "Resgate deve ser em múltiplos de 100 pontos.",
                    List.of(new Violacao("pontos", "deve ser múltiplo de 100")));
        }
        long saldo = transacaoRepository.calcularSaldo(usuario.getId());
        if (saldo < pontos) {
            throw new RegraNegocioException("SALDO_PONTOS_INSUFICIENTE", "Saldo de pontos insuficiente.",
                    List.of(new Violacao("pontos", "Saldo disponível: " + saldo)));
        }
        String cupom = "RDN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        BigDecimal desconto = VALOR_POR_100_PONTOS.multiply(BigDecimal.valueOf(pontos / 100));
        transacaoRepository.save(new TransacaoFidelidade(usuario, null, TipoTransacaoFidelidade.RESGATE, -pontos,
                "Resgate cupom " + cupom + " (R$ " + desconto + ")"));
        auditoria.registrar(usuario.getId(), "FIDELIDADE_RESGATE", "USUARIO", usuario.getId(), "pontos=" + pontos + " cupom=" + cupom);
        return new ResgateResponse(pontos, saldo - pontos, desconto, cupom);
    }

    private SaldoResponse montarSaldo(Usuario u) {
        return new SaldoResponse(u.getId(), transacaoRepository.calcularSaldo(u.getId()),
                u.isConsentimentoFidelidade(), u.getDataConsentimento(), REGRA);
    }

    private Usuario buscarUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("USUARIO_NAO_ENCONTRADO", "Usuário não encontrado."));
    }
}
