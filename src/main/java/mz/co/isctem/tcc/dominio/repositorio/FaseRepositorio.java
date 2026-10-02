package mz.co.isctem.tcc.dominio.repositorio;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import mz.co.isctem.tcc.dominio.modelo.Fase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FaseRepositorio extends JpaRepository<Fase, Long> {

    Optional<Fase> findByTccIdAndNumero(Long tccId, int numero);

    List<Fase> findByTccIdOrderByNumeroAsc(Long tccId);

    /** RF10: fases cujo prazo foi ultrapassado sem aprovacao e ainda nao marcadas como em atraso. */
    @Query("""
            SELECT f FROM Fase f
            JOIN FETCH f.tcc t
            JOIN FETCH t.estudante e
            JOIN FETCH e.utilizador
            WHERE f.estado <> 'APROVADO'
              AND f.dataLimite IS NOT NULL
              AND f.dataLimite < :referencia
              AND f.emAtraso = FALSE
            """)
    List<Fase> encontrarPrazosUltrapassados(@Param("referencia") LocalDate referencia);

    /** RF09: fases cujo prazo termina dentro da janela indicada, para aviso antecipado. */
    @Query("""
            SELECT f FROM Fase f
            JOIN FETCH f.tcc t
            JOIN FETCH t.estudante e
            JOIN FETCH e.utilizador
            WHERE f.estado <> 'APROVADO'
              AND f.dataLimite BETWEEN :inicio AND :fim
            """)
    List<Fase> encontrarPrazosAExpirar(
            @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    /**
     * RF12: pares (numero da fase, data de inicio, data de aprovacao) das fases ja concluidas.
     *
     * <p>A media em dias e calculada em Java para nao depender de funcoes de data especificas do
     * PostgreSQL, que nao existem no H2 usado nos testes.
     */
    @Query("""
            SELECT f.numero, f.dataInicio, f.dataAprovacao
            FROM Fase f
            WHERE f.dataAprovacao IS NOT NULL AND f.dataInicio IS NOT NULL
            ORDER BY f.numero
            """)
    List<Object[]> duracoesDasFasesConcluidas();

    long countByEmAtrasoTrue();

    /** RF10: fases ainda nao aprovadas de um curso, alvo da aplicacao de um plano de prazos. */
    @Query("""
            SELECT f FROM Fase f
            JOIN f.tcc t
            WHERE f.numero = :numeroFase
              AND LOWER(t.curso) = LOWER(:curso)
              AND f.estado <> 'APROVADO'
            """)
    List<Fase> porCursoEFaseNaoAprovadas(
            @Param("curso") String curso, @Param("numeroFase") int numeroFase);
}
