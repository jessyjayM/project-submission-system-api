package mz.co.isctem.tcc.dominio.repositorio;

import java.util.List;
import java.util.Optional;
import mz.co.isctem.tcc.dominio.enums.PerfilUtilizador;
import mz.co.isctem.tcc.dominio.modelo.Utilizador;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UtilizadorRepositorio extends JpaRepository<Utilizador, Long> {

    Optional<Utilizador> findByEmailIgnoreCase(String email);

    Optional<Utilizador> findByCodigoIgnoreCase(String codigo);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByCodigoIgnoreCase(String codigo);

    List<Utilizador> findByTipo(PerfilUtilizador tipo);

    /** RF02: o login aceita email institucional ou codigo de estudante no mesmo campo. */
    @Query("""
            SELECT u FROM Utilizador u
            WHERE LOWER(u.email) = LOWER(:credencial)
               OR LOWER(u.codigo) = LOWER(:credencial)
            """)
    Optional<Utilizador> findByCredencial(@Param("credencial") String credencial);

    /**
     * O {@code CAST(:termo AS string)} e obrigatorio: com o parametro a nulo, o driver JDBC envia-o
     * sem tipo e o PostgreSQL resolve-o como {@code bytea}, para o qual nao existe {@code lower()}.
     */
    @Query("""
            SELECT u FROM Utilizador u
            WHERE (:termo IS NULL
                   OR LOWER(u.nome) LIKE LOWER(CONCAT('%', CAST(:termo AS string), '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:termo AS string), '%'))
                   OR LOWER(u.codigo) LIKE LOWER(CONCAT('%', CAST(:termo AS string), '%')))
              AND (:tipo IS NULL OR u.tipo = :tipo)
            """)
    Page<Utilizador> pesquisar(
            @Param("termo") String termo, @Param("tipo") PerfilUtilizador tipo, Pageable pageable);
}
