package mz.co.isctem.tcc.dominio.repositorio;

import java.util.List;
import java.util.Optional;
import mz.co.isctem.tcc.dominio.modelo.Docente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DocenteRepositorio extends JpaRepository<Docente, Long> {

    Optional<Docente> findByUtilizadorId(Long utilizadorId);

    @Query("SELECT d FROM Docente d JOIN FETCH d.utilizador u ORDER BY u.nome")
    List<Docente> listarTodosComUtilizador();
}
