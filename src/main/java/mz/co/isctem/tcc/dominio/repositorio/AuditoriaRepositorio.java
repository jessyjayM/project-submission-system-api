package mz.co.isctem.tcc.dominio.repositorio;

import mz.co.isctem.tcc.dominio.modelo.Auditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditoriaRepositorio extends JpaRepository<Auditoria, Long> {

    @Query("""
            SELECT a FROM Auditoria a
            WHERE (:entidade IS NULL OR a.entidade = :entidade)
              AND (:accao IS NULL OR a.accao = :accao)
            ORDER BY a.criadoEm DESC
            """)
    Page<Auditoria> pesquisar(
            @Param("entidade") String entidade, @Param("accao") String accao, Pageable pageable);
}
