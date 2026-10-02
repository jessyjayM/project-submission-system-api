package mz.co.isctem.tcc.dominio.repositorio;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;

/**
 * Regressao das pesquisas com filtros opcionais a nulo.
 *
 * <p>Com o parametro a nulo, o driver JDBC envia-o sem tipo e o PostgreSQL resolve-o como
 * {@code bytea}; sem um CAST explicito, {@code lower(...)} rebenta com
 * "function lower(bytea) does not exist". A validacao de HQL no arranque nao apanha isto — so a
 * execucao real contra o PostgreSQL apanha —, dai este teste bater na base de dados verdadeira.
 *
 * <p>Requer a base de dados configurada em {@code application.yml}; sem ela o teste nao corre.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PesquisaComTermoNuloIT {

    @Autowired private UtilizadorRepositorio utilizadores;
    @Autowired private EstudanteRepositorio estudantes;

    @Test
    @DisplayName("A pesquisa de utilizadores sem termo nem tipo executa no PostgreSQL")
    void pesquisaDeUtilizadoresAceitaTermoNulo() {
        assertThatCode(() -> utilizadores.pesquisar(null, null, PageRequest.of(0, 20)).getContent())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("A pesquisa de utilizadores com termo preenchido continua a funcionar")
    void pesquisaDeUtilizadoresAceitaTermoPreenchido() {
        assertThatCode(() -> utilizadores.pesquisar("isctem", null, PageRequest.of(0, 20)).getContent())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("A pesquisa de estudantes sem filtros executa no PostgreSQL")
    void pesquisaDeEstudantesAceitaTermoNulo() {
        assertThatCode(
                        () ->
                                estudantes
                                        .pesquisar(null, null, false, PageRequest.of(0, 20))
                                        .getContent())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("A pesquisa de estudantes com termo e filtro de sem-supervisor executa")
    void pesquisaDeEstudantesComFiltros() {
        assertThatCode(
                        () ->
                                estudantes
                                        .pesquisar("sitoe", null, true, PageRequest.of(0, 20))
                                        .getContent())
                .doesNotThrowAnyException();
    }
}
