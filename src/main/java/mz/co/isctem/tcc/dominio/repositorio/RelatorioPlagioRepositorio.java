package mz.co.isctem.tcc.dominio.repositorio;

import java.util.List;
import java.util.Optional;
import mz.co.isctem.tcc.dominio.modelo.RelatorioPlagio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RelatorioPlagioRepositorio extends JpaRepository<RelatorioPlagio, Long> {

    Optional<RelatorioPlagio> findByVersaoDocumentoId(Long versaoDocumentoId);

    @Query("""
            SELECT r FROM RelatorioPlagio r
            JOIN FETCH r.versaoDocumento v
            JOIN FETCH v.submissao s
            WHERE s.id = :submissaoId
            ORDER BY v.numeroVersao DESC
            """)
    List<RelatorioPlagio> porSubmissao(Long submissaoId);

    /** RF12: estatisticas de plagio (total analisado, media e maximo de similaridade). */
    @Query("""
            SELECT COUNT(r), COALESCE(AVG(r.percentagemSimilaridade), 0), COALESCE(MAX(r.percentagemSimilaridade), 0)
            FROM RelatorioPlagio r
            WHERE r.estado = 'CONCLUIDO'
            """)
    List<Object[]> estatisticas();

    @Query("SELECT COUNT(r) FROM RelatorioPlagio r WHERE r.estado = 'CONCLUIDO' AND r.percentagemSimilaridade >= :limiar")
    long contarAcimaDe(java.math.BigDecimal limiar);
}
