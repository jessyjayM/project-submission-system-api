package mz.co.isctem.tcc.dominio.repositorio;

import java.util.List;
import mz.co.isctem.tcc.dominio.enums.DecisaoRevisao;
import mz.co.isctem.tcc.dominio.modelo.Revisao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RevisaoRepositorio extends JpaRepository<Revisao, Long> {

    List<Revisao> findBySubmissaoIdOrderByDataRevisaoAsc(Long submissaoId);

    long countByDecisao(DecisaoRevisao decisao);

    /** RF12: desempenho por supervisor (id, nome, total de revisoes, aprovacoes). */
    @Query("""
            SELECT d.id, u.nome, COUNT(r.id),
                   SUM(CASE WHEN r.decisao = 'APROVADO' THEN 1 ELSE 0 END)
            FROM Revisao r
            JOIN r.revisor ru
            JOIN Docente d ON d.utilizador = ru
            JOIN d.utilizador u
            GROUP BY d.id, u.nome
            ORDER BY COUNT(r.id) DESC
            """)
    List<Object[]> desempenhoPorSupervisor();
}
