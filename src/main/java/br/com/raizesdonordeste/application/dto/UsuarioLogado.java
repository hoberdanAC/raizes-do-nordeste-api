package br.com.raizesdonordeste.application.dto;

import br.com.raizesdonordeste.domain.enums.Perfil;

/**
 * Dados do usuário extraídos do JWT a cada requisição (principal do Spring Security).
 * Propositalmente mínimo: só id, perfil e unidade. E-mail/nome não trafegam no token (LGPD).
 */
public record UsuarioLogado(Long id, Perfil perfil, Long unidadeId) {

    public boolean is(Perfil p) { return perfil == p; }

    /** Funcionário de loja (escopo limitado à própria unidade). */
    public boolean isFuncionarioDeUnidade() {
        return perfil == Perfil.ATENDENTE || perfil == Perfil.COZINHA || perfil == Perfil.GERENTE;
    }
}
