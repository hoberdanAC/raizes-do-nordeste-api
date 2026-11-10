package br.com.raizesdonordeste.application.service;

import br.com.raizesdonordeste.application.dto.FidelidadeDtos.LogAuditoriaResponse;
import br.com.raizesdonordeste.application.dto.Pagina;
import br.com.raizesdonordeste.application.dto.Paginacao;
import br.com.raizesdonordeste.domain.model.LogAuditoria;
import br.com.raizesdonordeste.infrastructure.repository.LogAuditoriaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro de auditoria de ações sensíveis (RNF: logs/auditoria).
 * Grava no banco (consultável via GET /auditoria) e também no log da aplicação.
 * Participa da mesma transação do caso de uso: se o pedido falhar, o log também é desfeito,
 * evitando registro de algo que não aconteceu.
 */
@Service
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    private final LogAuditoriaRepository repository;

    public AuditoriaService(LogAuditoriaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void registrar(Long usuarioId, String acao, String entidade, Long entidadeId, String detalhes) {
        repository.save(new LogAuditoria(usuarioId, acao, entidade, entidadeId, detalhes));
        log.info("[AUDITORIA] usuario={} acao={} {}={} {}", usuarioId, acao, entidade, entidadeId, detalhes);
    }

    @Transactional(readOnly = true)
    public Pagina<LogAuditoriaResponse> listar(String acao, Integer page, Integer limit) {
        var pageable = Paginacao.de(page, limit, Sort.by(Sort.Direction.DESC, "criadoEm"));
        Page<LogAuditoria> pagina = (acao == null || acao.isBlank())
                ? repository.findAll(pageable)
                : repository.findByAcao(acao.toUpperCase(), pageable);
        return Pagina.de(pagina, LogAuditoriaResponse::de);
    }

    /** Mascara e-mail antes de ir para o log (ex.: c******@raizes.com). Minimização LGPD. */
    public static String mascararEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int arroba = email.indexOf('@');
        return email.charAt(0) + "******" + email.substring(arroba);
    }
}
