package mz.co.isctem.tcc.dominio.repositorio;

import java.util.List;
import mz.co.isctem.tcc.dominio.modelo.VersaoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VersaoDocumentoRepositorio extends JpaRepository<VersaoDocumento, Long> {

    List<VersaoDocumento> findBySubmissaoIdOrderByNumeroVersaoDesc(Long submissaoId);

    @Query("SELECT COALESCE(MAX(v.numeroVersao), 0) FROM VersaoDocumento v WHERE v.submissao.id = :submissaoId")
    int ultimoNumeroVersao(@Param("submissaoId") Long submissaoId);

    /**
     * RF08: corpus interno de comparacao.
     *
     * <p>Exclui as versoes do proprio TCC (para nao acusar semelhanca com as suas versoes
     * anteriores) e as que nao tenham texto extraido.
     */
    @Query("""
            SELECT v FROM VersaoDocumento v
            JOIN FETCH v.submissao s
            JOIN FETCH s.fase f
            JOIN FETCH f.tcc t
            JOIN FETCH t.estudante e
            JOIN FETCH e.utilizador
            WHERE v.textoExtraido IS NOT NULL
              AND LENGTH(v.textoExtraido) > 0
              AND t.id <> :tccId
            """)
    List<VersaoDocumento> corpusExcluindoTcc(@Param("tccId") Long tccId);
}
