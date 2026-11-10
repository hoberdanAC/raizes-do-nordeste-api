package br.com.raizesdonordeste.infrastructure.seed;

import br.com.raizesdonordeste.domain.enums.Perfil;
import br.com.raizesdonordeste.domain.model.Unidade;
import br.com.raizesdonordeste.domain.model.Usuario;
import br.com.raizesdonordeste.infrastructure.repository.UnidadeRepository;
import br.com.raizesdonordeste.infrastructure.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seed de usuários de teste (um por perfil). Roda na inicialização, apenas se ainda não existirem.
 * Fica em Java (e não em SQL do Flyway) porque as senhas precisam passar pelo BCrypt.
 * Senhas de DEMONSTRAÇÃO - nunca usar em produção.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UsuarioRepository usuarioRepository;
    private final UnidadeRepository unidadeRepository;
    private final PasswordEncoder encoder;

    public DataSeeder(UsuarioRepository usuarioRepository, UnidadeRepository unidadeRepository, PasswordEncoder encoder) {
        this.usuarioRepository = usuarioRepository;
        this.unidadeRepository = unidadeRepository;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (usuarioRepository.existsByEmail("admin@raizes.com")) {
            return;
        }
        Unidade recife = unidadeRepository.findById(1L).orElseThrow();   // criada na migration V2
        Unidade salvador = unidadeRepository.findById(2L).orElseThrow();

        criar("Administrador da Rede", "admin@raizes.com", "Admin@123", Perfil.ADMIN, null, false);
        criar("Gerente Recife", "gerente@raizes.com", "Gerente@123", Perfil.GERENTE, recife, false);
        criar("Atendente Recife", "atendente@raizes.com", "Atendente@123", Perfil.ATENDENTE, recife, false);
        criar("Cozinha Recife", "cozinha@raizes.com", "Cozinha@123", Perfil.COZINHA, recife, false);
        criar("Cozinha Salvador", "cozinha.salvador@raizes.com", "Cozinha@123", Perfil.COZINHA, salvador, false);
        criar("Maria Cliente", "cliente@raizes.com", "Cliente@123", Perfil.CLIENTE, null, true);
        criar("João Cliente", "joao@raizes.com", "Cliente@123", Perfil.CLIENTE, null, false);

        log.info("Seed de usuários criado (ver README para as credenciais de teste).");
    }

    private void criar(String nome, String email, String senha, Perfil perfil, Unidade unidade, boolean consentimento) {
        Usuario u = new Usuario(nome, email, encoder.encode(senha), perfil, unidade);
        if (consentimento) u.definirConsentimentoFidelidade(true);
        usuarioRepository.save(u);
    }
}
