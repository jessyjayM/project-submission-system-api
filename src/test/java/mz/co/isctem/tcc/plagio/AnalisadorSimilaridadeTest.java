package mz.co.isctem.tcc.plagio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** RF08: comportamento do calculo TF-IDF + similaridade de cosseno. */
class AnalisadorSimilaridadeTest {

    private final AnalisadorSimilaridade analisador = new AnalisadorSimilaridade();

    private static final String MONOGRAFIA_A =
            """
            A presente monografia descreve o desenvolvimento de um sistema de gestao de workflow
            para submissao e aprovacao de trabalhos de conclusao de curso. A metodologia adoptada
            assenta na prototipagem incremental com validacao junto dos supervisores do ISCTEM.
            O sistema controla fases, submissoes, versoes de documentos e prazos institucionais.
            """;

    private static final String MONOGRAFIA_B_COPIA_PARCIAL =
            """
            Esta monografia descreve o desenvolvimento de um sistema de gestao de workflow para
            submissao e aprovacao de trabalhos de conclusao de curso. A metodologia adoptada
            assenta na prototipagem incremental com validacao junto dos supervisores do ISCTEM.
            Foram acrescentados relatorios de acompanhamento.
            """;

    private static final String MONOGRAFIA_C_TEMA_DIFERENTE =
            """
            O presente trabalho analisa o impacto da microfinanca no rendimento das familias rurais
            da provincia de Inhambane. Recorreu-se a inqueritos por questionario aplicados a
            duzentos agregados familiares e a regressao linear multipla para estimar os efeitos.
            """;

    @Test
    @DisplayName("Deteca similaridade elevada entre um documento e a sua copia parcial")
    void documentosQuaseIguaisApresentamSimilaridadeElevada() {
        List<AnalisadorSimilaridade.Semelhanca> resultado =
                analisador.comparar(
                        MONOGRAFIA_A,
                        List.of(new AnalisadorSimilaridade.DocumentoCorpus(1L, MONOGRAFIA_B_COPIA_PARCIAL)),
                        5);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.getFirst().similaridade()).isGreaterThan(0.5);
    }

    @Test
    @DisplayName("Documentos sobre temas distintos apresentam similaridade baixa")
    void documentosDeTemasDiferentesApresentamSimilaridadeBaixa() {
        List<AnalisadorSimilaridade.Semelhanca> resultado =
                analisador.comparar(
                        MONOGRAFIA_A,
                        List.of(new AnalisadorSimilaridade.DocumentoCorpus(1L, MONOGRAFIA_C_TEMA_DIFERENTE)),
                        5);

        double similaridade = resultado.isEmpty() ? 0 : resultado.getFirst().similaridade();
        assertThat(similaridade).isLessThan(0.2);
    }

    @Test
    @DisplayName("Ordena as semelhancas por ordem decrescente")
    void ordenaResultadosPorSimilaridadeDecrescente() {
        List<AnalisadorSimilaridade.Semelhanca> resultado =
                analisador.comparar(
                        MONOGRAFIA_A,
                        List.of(
                                new AnalisadorSimilaridade.DocumentoCorpus(1L, MONOGRAFIA_C_TEMA_DIFERENTE),
                                new AnalisadorSimilaridade.DocumentoCorpus(2L, MONOGRAFIA_B_COPIA_PARCIAL)),
                        5);

        assertThat(resultado).isNotEmpty();
        assertThat(resultado.getFirst().id()).isEqualTo(2L);
        assertThat(resultado)
                .isSortedAccordingTo((a, b) -> Double.compare(b.similaridade(), a.similaridade()));
    }

    @Test
    @DisplayName("Texto demasiado curto nao produz analise")
    void textoCurtoNaoProduzResultado() {
        assertThat(
                        analisador.comparar(
                                "Introducao breve.",
                                List.of(new AnalisadorSimilaridade.DocumentoCorpus(1L, MONOGRAFIA_A)),
                                20))
                .isEmpty();
    }

    @Test
    @DisplayName("Corpus vazio nao produz analise")
    void corpusVazioNaoProduzResultado() {
        assertThat(analisador.comparar(MONOGRAFIA_A, List.of(), 5)).isEmpty();
    }

    @Test
    @DisplayName("A tokenizacao remove acentos e palavras vazias do portugues")
    void tokenizacaoRemoveAcentosEPalavrasVazias() {
        List<String> termos = analisador.tokenizar("A submissão do relatório e a metodologia de análise");

        assertThat(termos).contains("submissao", "relatorio", "metodologia", "analise");
        assertThat(termos).doesNotContain("a", "do", "de");
    }
}
