package br.com.raizesdonordeste.infrastructure.repository;

import br.com.raizesdonordeste.domain.model.LogAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long> {
    Page<LogAuditoria> findByAcao(String acao, Pageable pageable);
}
