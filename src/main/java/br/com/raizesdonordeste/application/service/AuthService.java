package br.com.raizesdonordeste.application.service;

import br.com.raizesdonordeste.application.dto.AuthDtos.LoginRequest;
import br.com.raizesdonordeste.application.dto.AuthDtos.LoginResponse;
import br.com.raizesdonordeste.application.dto.AuthDtos.UsuarioResumo;
import br.com.raizesdonordeste.application.port.TokenService;
import br.com.raizesdonordeste.domain.exception.NaoAutenticadoException;
import br.com.raizesdonordeste.domain.model.Usuario;
import br.com.raizesdonordeste.infrastructure.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: autenticar usuário.
 * Compara a senha com o hash BCrypt e emite um JWT assinado.
 */
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuditoriaService auditoria;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                       TokenService tokenService, AuditoriaService auditoria) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.auditoria = auditoria;
    }

    // noRollbackFor: a tentativa de login com falha precisa ficar registrada na auditoria.
    @Transactional(noRollbackFor = NaoAutenticadoException.class)
    public LoginResponse login(LoginRequest req) {
        Usuario usuario = usuarioRepository.findByEmail(req.email().toLowerCase()).orElse(null);

        // Mesma mensagem para "e-mail não existe" e "senha errada": não revela quais e-mails estão cadastrados.
        if (usuario == null || !usuario.isAtivo() || !passwordEncoder.matches(req.senha(), usuario.getSenhaHash())) {
            auditoria.registrar(usuario != null ? usuario.getId() : null, "LOGIN_FALHA", "USUARIO",
                    usuario != null ? usuario.getId() : null, "email=" + AuditoriaService.mascararEmail(req.email()));
            throw new NaoAutenticadoException("E-mail ou senha inválidos.");
        }

        TokenService.TokenGerado token = tokenService.gerar(usuario);
        auditoria.registrar(usuario.getId(), "LOGIN_SUCESSO", "USUARIO", usuario.getId(), "perfil=" + usuario.getPerfil());

        return new LoginResponse(token.token(), "Bearer", token.expiraEmSegundos(),
                new UsuarioResumo(usuario.getId(), usuario.getNome(), usuario.getPerfil()));
    }
}
