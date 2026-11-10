package br.com.raizesdonordeste.application.service;

import br.com.raizesdonordeste.application.dto.UsuarioDtos.CadastroClienteRequest;
import br.com.raizesdonordeste.application.dto.UsuarioDtos.CadastroFuncionarioRequest;
import br.com.raizesdonordeste.application.dto.UsuarioDtos.UsuarioResponse;
import br.com.raizesdonordeste.application.dto.UsuarioLogado;
import br.com.raizesdonordeste.domain.enums.Perfil;
import br.com.raizesdonordeste.domain.exception.DadosInvalidosException;
import br.com.raizesdonordeste.domain.exception.RecursoNaoEncontradoException;
import br.com.raizesdonordeste.domain.exception.RegraNegocioException;
import br.com.raizesdonordeste.domain.exception.Violacao;
import br.com.raizesdonordeste.domain.model.Unidade;
import br.com.raizesdonordeste.domain.model.Usuario;
import br.com.raizesdonordeste.infrastructure.repository.UnidadeRepository;
import br.com.raizesdonordeste.infrastructure.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

/** Casos de uso de usuário: cadastro, consulta dos próprios dados e anonimização (LGPD). */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UnidadeRepository unidadeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoria;

    public UsuarioService(UsuarioRepository usuarioRepository, UnidadeRepository unidadeRepository,
                          PasswordEncoder passwordEncoder, AuditoriaService auditoria) {
        this.usuarioRepository = usuarioRepository;
        this.unidadeRepository = unidadeRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoria = auditoria;
    }

    /** Cadastro público: sempre cria perfil CLIENTE (ninguém se autopromove a ADMIN). */
    @Transactional
    public UsuarioResponse cadastrarCliente(CadastroClienteRequest req) {
        validarEmailUnico(req.email());
        // BCrypt gera salt aleatório por senha (protege contra rainbow tables e força bruta).
        Usuario usuario = new Usuario(req.nome().trim(), req.email(), passwordEncoder.encode(req.senha()), Perfil.CLIENTE, null);
        if (Boolean.TRUE.equals(req.consentimentoFidelidade())) {
            usuario.definirConsentimentoFidelidade(true);
        }
        usuarioRepository.save(usuario);
        auditoria.registrar(usuario.getId(), "USUARIO_CADASTRADO", "USUARIO", usuario.getId(),
                "perfil=CLIENTE consentimentoFidelidade=" + usuario.isConsentimentoFidelidade());
        return UsuarioResponse.de(usuario);
    }

    /** Cadastro de funcionário (somente ADMIN). Funcionário de loja precisa de unidade. */
    @Transactional
    public UsuarioResponse cadastrarFuncionario(CadastroFuncionarioRequest req, UsuarioLogado admin) {
        validarEmailUnico(req.email());
        Unidade unidade = null;
        boolean exigeUnidade = req.perfil() == Perfil.ATENDENTE || req.perfil() == Perfil.COZINHA || req.perfil() == Perfil.GERENTE;
        if (exigeUnidade && req.unidadeId() == null) {
            throw new DadosInvalidosException("VALIDACAO_FALHOU", "Funcionário de loja precisa estar vinculado a uma unidade.",
                    List.of(new Violacao("unidadeId", "obrigatório para o perfil " + req.perfil())));
        }
        if (req.unidadeId() != null) {
            unidade = unidadeRepository.findById(req.unidadeId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("UNIDADE_NAO_ENCONTRADA", "Unidade não encontrada."));
        }
        Usuario usuario = new Usuario(req.nome().trim(), req.email(), passwordEncoder.encode(req.senha()), req.perfil(), unidade);
        usuarioRepository.save(usuario);
        auditoria.registrar(admin.id(), "FUNCIONARIO_CADASTRADO", "USUARIO", usuario.getId(), "perfil=" + req.perfil());
        return UsuarioResponse.de(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse meusDados(UsuarioLogado logado) {
        return UsuarioResponse.de(buscar(logado.id()));
    }

    /**
     * Direito de eliminação/anonimização (LGPD art. 18). Os pedidos são mantidos
     * (obrigação fiscal), mas deixam de apontar para uma pessoa identificável.
     */
    @Transactional
    public void anonimizar(UsuarioLogado logado) {
        Usuario usuario = buscar(logado.id());
        usuario.anonimizar(passwordEncoder.encode(UUID.randomUUID().toString()));
        auditoria.registrar(logado.id(), "USUARIO_ANONIMIZADO", "USUARIO", logado.id(), "solicitado pelo titular");
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("USUARIO_NAO_ENCONTRADO", "Usuário não encontrado."));
    }

    private void validarEmailUnico(String email) {
        if (usuarioRepository.existsByEmail(email.toLowerCase())) {
            // E-mail é único: impede contas duplicadas.
            throw new RegraNegocioException("EMAIL_JA_CADASTRADO", "Já existe um usuário com este e-mail.",
                    List.of(new Violacao("email", "já cadastrado")));
        }
    }
}
