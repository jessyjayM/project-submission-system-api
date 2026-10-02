package mz.co.isctem.tcc.dominio.repositorio;

import java.util.List;
import java.util.Optional;
import mz.co.isctem.tcc.dominio.modelo.Estudante;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EstudanteRepositorio extends JpaRepository<Estudante, Long> {

    Optional<Estudante> findByUtilizadorId(Long utilizadorId);

    List<Estudante> findBySupervisorId(Long supervisorId);

    long countBySupervisorIsNull();

    /**
     * O {@code CAST(:termo AS string)} e obrigatorio: com o parametro a nulo, o driver JDBC envia-o
     * sem tipo e o PostgreSQL resolve-o como {@code bytea}, para o qual nao existe {@code lower()}.
     */
    @Query("""
            SELECT e FROM Estudante e
            JOIN FETCH e.utilizador u
            LEFT JOIN FETCH e.supervisor s
            WHERE (:termo IS NULL
                   OR LOWER(u.nome) LIKE LOWER(CONCAT('%', CAST(:termo AS string), '%'))
                   OR LOWER(u.codigo) LIKE LOWER(CONCAT('%', CAST(:termo AS string), '%')))
              AND (:supervisorId IS NULL OR s.id = :supervisorId)
              AND (:semSupervisor = FALSE OR e.supervisor IS NULL)
            """)
    Page<Estudante> pesquisar(
            @Param("termo") String termo,
            @Param("supervisorId") Long supervisorId,
            @Param("semSupervisor") boolean semSupervisor,
            Pageable pageable);

    /** RF11: numero de orientandos por supervisor. */
    @Query("""
            SELECT s.id, u.nome, COUNT(e.id)
            FROM Estudante e
            JOIN e.supervisor s
            JOIN s.utilizador u
            GROUP BY s.id, u.nome
            ORDER BY COUNT(e.id) DESC
            """)
    List<Object[]> contarPorSupervisor();
}
