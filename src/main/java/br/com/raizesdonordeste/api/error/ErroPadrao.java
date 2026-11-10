package br.com.raizesdonordeste.api.error;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Formato ÚNICO de erro da API (seção 5.3/5.4 do roteiro). Toda falha (400, 401, 402, 403,
 * 404, 409, 422, 500, 503) responde neste formato, inclusive as geradas pelo Spring Security.
 */
public record ErroPadrao(
        String error,
        String message,
        List<DetalheErro> details,
        String timestamp,
        String path,
        String requestId) {

    public record DetalheErro(String field, String issue) { }

    public static ErroPadrao de(String error, String message, List<DetalheErro> details, String path) {
        return new ErroPadrao(error, message, details == null ? List.of() : details,
                OffsetDateTime.now(ZoneOffset.UTC).withNano(0).toString(), path, UUID.randomUUID().toString());
    }
}
