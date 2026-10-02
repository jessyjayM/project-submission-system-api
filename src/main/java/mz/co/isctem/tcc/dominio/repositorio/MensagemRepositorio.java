package mz.co.isctem.tcc.dominio.repositorio;

import java.util.List;
import mz.co.isctem.tcc.dominio.modelo.Mensagem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MensagemRepositorio extends JpaRepository<Mensagem, Long> {

    @Query(
            value = "SELECT m FROM Mensagem m JOIN FETCH m.remetente WHERE m.tcc.id = :tccId",
            countQuery = "SELECT COUNT(m) FROM Mensagem m WHERE m.tcc.id = :tccId")
    Page<Mensagem> porTcc(@Param("tccId") Long tccId, Pageable pageable);

    /** Mensagens posteriores ao id indicado, usadas pelo polling do chat. */
    @Query("SELECT m FROM Mensagem m JOIN FETCH m.remetente WHERE m.tcc.id = :tccId AND m.id > :desdeId ORDER BY m.id ASC")
    List<Mensagem> novasDesde(@Param("tccId") Long tccId, @Param("desdeId") Long desdeId);

    @Modifying
    @Query("UPDATE Mensagem m SET m.lidaEm = CURRENT_TIMESTAMP WHERE m.tcc.id = :tccId AND m.remetente.id <> :leitorId AND m.lidaEm IS NULL")
    int marcarComoLidas(@Param("tccId") Long tccId, @Param("leitorId") Long leitorId);

    @Query("SELECT COUNT(m) FROM Mensagem m WHERE m.tcc.id = :tccId AND m.remetente.id <> :leitorId AND m.lidaEm IS NULL")
    long contarNaoLidas(@Param("tccId") Long tccId, @Param("leitorId") Long leitorId);
}
