package mz.co.isctem.tcc.dominio.repositorio;

import java.util.List;
import java.util.Optional;
import mz.co.isctem.tcc.dominio.enums.EstadoTcc;
import mz.co.isctem.tcc.dominio.modelo.Tcc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TccRepositorio extends JpaRepository<Tcc, Long> {

    Optional<Tcc> findByEstudanteId(Long estudanteId);

    Optional<Tcc> findByEstudanteUtilizadorId(Long utilizadorId);

    List<Tcc> findByEstudanteSupervisorId(Long supervisorId);

    long countByEstadoGeral(EstadoTcc estado);

    /** RF11: distribuicao de estudantes por fase. */
    @Query("SELECT t.faseActual, COUNT(t) FROM Tcc t WHERE t.estadoGeral = 'EM_CURSO' GROUP BY t.faseActual")
    List<Object[]> contarPorFase();

    @Query("SELECT COUNT(DISTINCT t) FROM Tcc t JOIN t.fases f WHERE f.emAtraso = TRUE")
    long contarEmAtraso();
}
