package mz.co.isctem.tcc.dominio.repositorio;

import java.util.List;
import java.util.Optional;
import mz.co.isctem.tcc.dominio.modelo.Milestone;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MilestoneRepositorio extends JpaRepository<Milestone, Long> {

    List<Milestone> findByCursoIgnoreCaseAndAnoLectivoOrderByNumeroFaseAsc(String curso, int anoLectivo);

    Optional<Milestone> findByCursoIgnoreCaseAndAnoLectivoAndNumeroFase(
            String curso, int anoLectivo, int numeroFase);

    List<Milestone> findAllByOrderByCursoAscAnoLectivoDescNumeroFaseAsc();
}
