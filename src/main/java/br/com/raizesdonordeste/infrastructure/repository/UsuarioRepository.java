package br.com.raizesdonordeste.infrastructure.repository;

import br.com.raizesdonordeste.domain.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/** Acesso a dados de usuários (Spring Data JPA gera a implementação). */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
}
