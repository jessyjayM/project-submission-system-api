package mz.co.isctem.tcc.dominio.repositorio;

import java.time.Instant;
import java.util.Optional;
import mz.co.isctem.tcc.dominio.modelo.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepositorio extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revogado = TRUE WHERE r.utilizador.id = :utilizadorId AND r.revogado = FALSE")
    int revogarTodosDoUtilizador(@Param("utilizadorId") Long utilizadorId);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiraEm < :referencia")
    int apagarExpirados(@Param("referencia") Instant referencia);
}
