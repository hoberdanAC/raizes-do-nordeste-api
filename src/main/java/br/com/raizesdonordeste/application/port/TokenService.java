package br.com.raizesdonordeste.application.port;

import br.com.raizesdonordeste.domain.model.Usuario;

/**
 * Porta (interface) para emissão de token. A aplicação não sabe que é JWT:
 * a implementação concreta fica na infraestrutura (JwtService).
 */
public interface TokenService {

    TokenGerado gerar(Usuario usuario);

    record TokenGerado(String token, long expiraEmSegundos) { }
}
