package br.com.raizesdonordeste.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Unidade (loja) da rede. Cada unidade possui estoque e cardápio próprios. */
@Entity
@Table(name = "unidades")
public class Unidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 80)
    private String cidade;

    @Column(nullable = false, length = 2)
    private String uf;

    @Column(nullable = false)
    private boolean ativa = true;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    protected Unidade() { } // exigido pelo JPA

    public Unidade(String nome, String cidade, String uf) {
        this.nome = nome;
        this.cidade = cidade;
        this.uf = uf.toUpperCase();
        this.criadoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getCidade() { return cidade; }
    public String getUf() { return uf; }
    public boolean isAtiva() { return ativa; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
