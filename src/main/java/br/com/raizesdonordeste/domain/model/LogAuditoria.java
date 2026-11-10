package br.com.raizesdonordeste.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Registro de auditoria de ações sensíveis (quem fez, o quê, em qual registro, quando).
 * Não guarda dados pessoais em texto livre (e-mails são mascarados antes de gravar).
 */
@Entity
@Table(name = "logs_auditoria")
public class LogAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(nullable = false, length = 40)
    private String acao;

    @Column(nullable = false, length = 40)
    private String entidade;

    @Column(name = "entidade_id")
    private Long entidadeId;

    @Column(length = 500)
    private String detalhes;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    protected LogAuditoria() { }

    public LogAuditoria(Long usuarioId, String acao, String entidade, Long entidadeId, String detalhes) {
        this.usuarioId = usuarioId;
        this.acao = acao;
        this.entidade = entidade;
        this.entidadeId = entidadeId;
        this.detalhes = detalhes != null && detalhes.length() > 500 ? detalhes.substring(0, 500) : detalhes;
        this.criadoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public String getAcao() { return acao; }
    public String getEntidade() { return entidade; }
    public Long getEntidadeId() { return entidadeId; }
    public String getDetalhes() { return detalhes; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
