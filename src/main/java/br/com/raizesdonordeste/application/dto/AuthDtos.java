package br.com.raizesdonordeste.application.dto;

import br.com.raizesdonordeste.domain.enums.Perfil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Contratos de autenticação (login e resposta com token). */
public final class AuthDtos {

    private AuthDtos() { }

    public record LoginRequest(
            @Schema(example = "cliente@raizes.com") @NotBlank @Email String email,
            @Schema(example = "Cliente@123") @NotBlank String senha) { }

    public record LoginResponse(
            String accessToken,
            String tokenType,
            long expiresIn,
            UsuarioResumo usuario) { }

    public record UsuarioResumo(Long id, String nome, Perfil perfil) { }
}
