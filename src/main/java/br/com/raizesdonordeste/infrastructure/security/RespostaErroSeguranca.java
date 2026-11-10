package br.com.raizesdonordeste.infrastructure.security;

import br.com.raizesdonordeste.api.error.ErroPadrao;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Faz o Spring Security devolver 401/403 no MESMO formato JSON padrão da API
 * (sem isso ele devolveria uma resposta vazia ou HTML).
 */
@Component
public class RespostaErroSeguranca implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RespostaErroSeguranca(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 401 - sem token, token expirado ou assinatura inválida. */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) throws IOException {
        escrever(response, 401, ErroPadrao.de("NAO_AUTENTICADO",
                "Token de acesso ausente, inválido ou expirado.", null, request.getRequestURI()));
    }

    /** 403 - autenticado, mas sem o perfil exigido. */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) throws IOException {
        escrever(response, 403, ErroPadrao.de("ACESSO_NEGADO",
                "Seu perfil não tem permissão para esta operação.", null, request.getRequestURI()));
    }

    private void escrever(HttpServletResponse response, int status, ErroPadrao erro) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), erro);
    }
}
