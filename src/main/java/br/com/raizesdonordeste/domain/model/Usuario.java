package br.com.raizesdonordeste.domain.model;

import br.com.raizesdonordeste.domain.enums.Perfil;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Usuário do sistema (cliente ou funcionário).
 *
 * Decisões de projeto:
 *  - senha armazenada somente como hash BCrypt (salt automático por senha);
 *  - campo "perfil" para autorização por role;
 *  - CPF não é coletado (minimização de dados - LGPD);
 *  - consentimento da fidelidade registrado com data (LGPD - base legal: consentimento).
 */
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 150, unique = true)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Perfil perfil;

    /** Unidade em que o funcionário trabalha (nulo para clientes e ADMIN da rede). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidade_id")
    private Unidade unidade;

    @Column(name = "consentimento_fidelidade", nullable = false)
    private boolean consentimentoFidelidade;

    @Column(name = "data_consentimento")
    private LocalDateTime dataConsentimento;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    protected Usuario() { }

    public Usuario(String nome, String email, String senhaHash, Perfil perfil, Unidade unidade) {
        this.nome = nome;
        this.email = email.toLowerCase();
        this.senhaHash = senhaHash;
        this.perfil = perfil;
        this.unidade = unidade;
        this.criadoEm = LocalDateTime.now();
    }

    /** Registra (ou revoga) o consentimento, guardando quando aconteceu. */
    public void definirConsentimentoFidelidade(boolean aceito) {
        this.consentimentoFidelidade = aceito;
        this.dataConsentimento = LocalDateTime.now();
    }

    /**
     * Anonimização (direito do titular - LGPD art. 18). Remove dados que identificam a pessoa,
     * mas mantém o registro para preservar o histórico de pedidos (obrigação legal/fiscal).
     */
    public void anonimizar(String senhaHashAleatoria) {
        this.nome = "Usuário anonimizado";
        this.email = "anonimizado-" + id + "@anonimizado.local";
        this.senhaHash = senhaHashAleatoria;
        this.consentimentoFidelidade = false;
        this.dataConsentimento = LocalDateTime.now();
        this.ativo = false;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getSenhaHash() { return senhaHash; }
    public Perfil getPerfil() { return perfil; }
    public Unidade getUnidade() { return unidade; }
    public boolean isConsentimentoFidelidade() { return consentimentoFidelidade; }
    public LocalDateTime getDataConsentimento() { return dataConsentimento; }
    public boolean isAtivo() { return ativo; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
