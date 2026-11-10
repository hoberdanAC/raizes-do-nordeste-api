package br.com.raizesdonordeste.infrastructure.security;

import br.com.raizesdonordeste.application.dto.UsuarioLogado;
import br.com.raizesdonordeste.application.port.TokenService;
import br.com.raizesdonordeste.domain.enums.Perfil;
import br.com.raizesdonordeste.domain.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Emissão e validação de JWT assinado com HMAC-SHA256.
 * A assinatura impede que o token seja forjado e a expiração limita o tempo de uso
 * de um token vazado.
 *
 * Claims: sub = id do usuário, perfil, unidadeId. Sem nome/e-mail (minimização LGPD).
 */
@Service
public class JwtService implements TokenService {

    private static final String ISSUER = "raizes-do-nordeste-api";

    private final SecretKey chave;
    private final long expiracaoSegundos;

    public JwtService(@Value("${app.jwt.secret}") String segredo,
                      @Value("${app.jwt.expiration-seconds}") long expiracaoSegundos) {
        if (segredo.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET precisa ter pelo menos 32 caracteres.");
        }
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        this.expiracaoSegundos = expiracaoSegundos;
    }

    @Override
    public TokenGerado gerar(Usuario usuario) {
        Instant agora = Instant.now();
        var builder = Jwts.builder()
                .issuer(ISSUER)
                .subject(String.valueOf(usuario.getId()))
                .claim("perfil", usuario.getPerfil().name())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plusSeconds(expiracaoSegundos)));
        if (usuario.getUnidade() != null) {
            builder.claim("unidadeId", usuario.getUnidade().getId());
        }
        return new TokenGerado(builder.signWith(chave).compact(), expiracaoSegundos);
    }

    /** Valida assinatura/expiração. Token inválido = Optional vazio (vira 401 no filtro). */
    public Optional<UsuarioLogado> validar(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(chave).requireIssuer(ISSUER).build()
                    .parseSignedClaims(token).getPayload();
            Number unidade = claims.get("unidadeId", Number.class);
            return Optional.of(new UsuarioLogado(
                    Long.valueOf(claims.getSubject()),
                    Perfil.valueOf(claims.get("perfil", String.class)),
                    unidade != null ? unidade.longValue() : null));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
