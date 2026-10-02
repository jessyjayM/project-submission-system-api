package mz.co.isctem.tcc.dominio.repositorio;

import java.util.List;
import java.util.Optional;
import mz.co.isctem.tcc.dominio.enums.EstadoSubmissao;
import mz.co.isctem.tcc.dominio.modelo.Submissao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubmissaoRepositorio extends JpaRepository<Submissao, Long> {

    List<Submissao> findByFaseIdOrderByNumeroDesc(Long faseId);

    Optional<Submissao> findFirstByFaseIdAndEstadoOrderByNumeroDesc(Long faseId, EstadoSubmissao estado);

    @Query("SELECT COALESCE(MAX(s.numero), 0) FROM Submissao s WHERE s.fase.id = :faseId")
    int ultimoNumero(@Param("faseId") Long faseId);

    /** RF11: submissoes a aguardar decisao de um determinado supervisor. */
    @Query("""
            SELECT s FROM Submissao s
            JOIN FETCH s.fase f
            JOIN FETCH f.tcc t
            JOIN FETCH t.estudante e
            JOIN FETCH e.utilizador
            WHERE s.estado = 'EM_REVISAO' AND e.supervisor.id = :supervisorId
            ORDER BY s.dataSubmissao ASC
            """)
    List<Submissao> pendentesDoSupervisor(@Param("supervisorId") Long supervisorId);

    /**
     * RF11: pares (data de submissao, data de decisao) para calcular o tempo medio de resposta.
     * Quando {@code supervisorId} e nulo, abrange toda a instituicao.
     */
    @Query("""
            SELECT s.dataSubmissao, s.dataDecisao
            FROM Submissao s
            JOIN s.fase f
            JOIN f.tcc t
            JOIN t.estudante e
            WHERE s.dataDecisao IS NOT NULL
              AND (:supervisorId IS NULL OR e.supervisor.id = :supervisorId)
            """)
    List<Object[]> temposDeResposta(@Param("supervisorId") Long supervisorId);

    long countByEstado(EstadoSubmissao estado);
}
