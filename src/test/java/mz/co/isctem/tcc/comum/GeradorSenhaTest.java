package mz.co.isctem.tcc.comum;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** RF03: caracteristicas das senhas temporarias. */
class GeradorSenhaTest {

    @Test
    @DisplayName("A senha temporaria respeita o minimo de 8 caracteres exigido pelo RF03")
    void respeitaComprimentoMinimo() {
        assertThat(GeradorSenha.gerar()).hasSizeGreaterThanOrEqualTo(8);
        assertThatThrownBy(() -> GeradorSenha.gerar(6))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("A senha contem maiuscula, minuscula, digito e simbolo")
    void contemTodasAsClassesDeCaracteres() {
        String senha = GeradorSenha.gerar(12);

        assertThat(senha.chars().anyMatch(Character::isUpperCase)).isTrue();
        assertThat(senha.chars().anyMatch(Character::isLowerCase)).isTrue();
        assertThat(senha.chars().anyMatch(Character::isDigit)).isTrue();
        assertThat(senha.chars().anyMatch(c -> "@#$%&*!".indexOf(c) >= 0)).isTrue();
    }

    @Test
    @DisplayName("Nao usa caracteres ambiguos, por serem comunicados verbalmente")
    void evitaCaracteresAmbiguos() {
        String todas = IntStream.range(0, 200).mapToObj(i -> GeradorSenha.gerar()).reduce("", String::concat);
        assertThat(todas).doesNotContain("0", "O", "1", "l", "I");
    }

    @Test
    @DisplayName("Gera senhas distintas em invocacoes sucessivas")
    void geraSenhasDistintas() {
        assertThat(IntStream.range(0, 50).mapToObj(i -> GeradorSenha.gerar()).distinct().count())
                .isEqualTo(50);
    }
}
