package br.com.raizesdonordeste.application.dto;

import br.com.raizesdonordeste.domain.enums.Perfil;
import br.com.raizesdonordeste.domain.model.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/** Contratos de usuário (cadastro sem CPF - minimização LGPD). */
public final class UsuarioDtos {

    private UsuarioDtos() { }

    public record CadastroClienteRequest(
            @Schema(example = "Maria da Silva") @NotBlank @Size(max = 120) String nome,
            @Schema(example = "maria@email.com") @NotBlank @Email @Size(max = 150) String email,
            @Schema(example = "Senha@123", description = "Mínimo 8 caracteres")
            @NotBlank @Size(min = 8, max = 72) String senha,
            @Schema(example = "true", description = "Consentimento LGPD para o programa de fidelidade (opcional)")
            Boolean consentimentoFidelidade) { }

    public record CadastroFuncionarioRequest(
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(min = 8, max = 72) String senha,
            @NotNull Perfil perfil,
            @Schema(description = "Obrigatório para ATENDENTE, COZINHA e GERENTE") Long unidadeId) { }

    /** Resposta SEM senhaHash: o hash da senha nunca é exposto. */
    public record UsuarioResponse(Long id, String nome, String email, Perfil perfil, Long unidadeId,
                                  boolean consentimentoFidelidade, LocalDateTime dataConsentimento,
                                  LocalDateTime criadoEm) {
        public static UsuarioResponse de(Usuario u) {
            return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getPerfil(),
                    u.getUnidade() != null ? u.getUnidade().getId() : null,
                    u.isConsentimentoFidelidade(), u.getDataConsentimento(), u.getCriadoEm());
        }
    }
}
