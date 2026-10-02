package mz.co.isctem.tcc.dominio.repositorio;

import mz.co.isctem.tcc.dominio.modelo.Notificacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificacaoRepositorio extends JpaRepository<Notificacao, Long> {

    Page<Notificacao> findByDestinatarioIdOrderByCriadaEmDesc(Long destinatarioId, Pageable pageable);

    long countByDestinatarioIdAndLidaFalse(Long destinatarioId);

    @Modifying
    @Query("UPDATE Notificacao n SET n.lida = TRUE WHERE n.destinatario.id = :destinatarioId AND n.lida = FALSE")
    int marcarTodasComoLidas(@Param("destinatarioId") Long destinatarioId);
}
