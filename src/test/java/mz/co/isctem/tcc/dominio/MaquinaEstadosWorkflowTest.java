package mz.co.isctem.tcc.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import mz.co.isctem.tcc.dominio.enums.AreaCurso;
import mz.co.isctem.tcc.dominio.enums.EstadoFase;
import mz.co.isctem.tcc.dominio.enums.FaseTcc;
import mz.co.isctem.tcc.dominio.modelo.Fase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Seccao 5.4: maquina de estados do workflow e regras das fases. */
class MaquinaEstadosWorkflowTest {

    @Test
    @DisplayName("Cursos de Informatica tem cinco fases; os restantes terminam na Fase 4")
    void fase5ExisteApenasParaInformatica() {
        assertThat(FaseTcc.aplicaveis(AreaCurso.INFORMATICA))
                .hasSize(5)
                .contains(FaseTcc.PRONTIDAO_SISTEMA);
        assertThat(FaseTcc.aplicaveis(AreaCurso.OUTRA))
                .hasSize(4)
                .doesNotContain(FaseTcc.PRONTIDAO_SISTEMA);

        assertThat(FaseTcc.ultimaFase(AreaCurso.INFORMATICA)).isEqualTo(5);
        assertThat(FaseTcc.ultimaFase(AreaCurso.OUTRA)).isEqualTo(4);
    }

    @Test
    @DisplayName("O estudante pode submeter em Pendente e em Correcao Solicitada")
    void submissaoPermitidaNoCicloDeCorreccao() {
        assertThat(EstadoFase.PENDENTE.permiteSubmissao()).isTrue();
        assertThat(EstadoFase.CORRECAO_SOLICITADA.permiteSubmissao()).isTrue();
        // Enquanto aguarda avaliacao, nao aceita nova submissao.
        assertThat(EstadoFase.EM_REVISAO.permiteSubmissao()).isFalse();
        // Aprovado e a unica saida do ciclo.
        assertThat(EstadoFase.APROVADO.permiteSubmissao()).isFalse();
        assertThat(EstadoFase.APROVADO.terminal()).isTrue();
    }

    @Test
    @DisplayName("So uma submissao em revisao pode ser avaliada pelo supervisor")
    void decisaoApenasSobreSubmissaoEmRevisao() {
        assertThat(EstadoFase.EM_REVISAO.permiteDecisao()).isTrue();
        assertThat(EstadoFase.PENDENTE.permiteDecisao()).isFalse();
        assertThat(EstadoFase.CORRECAO_SOLICITADA.permiteDecisao()).isFalse();
        assertThat(EstadoFase.APROVADO.permiteDecisao()).isFalse();
    }

    @Test
    @DisplayName("Em Atraso acumula com o estado do fluxo em vez de o substituir")
    void atrasoEIndicadoEmParalelo() {
        Fase fase = Fase.builder().numero(2).nome("Capitulos Iniciais").estado(EstadoFase.EM_REVISAO).build();
        fase.setEmAtraso(true);

        assertThat(fase.getEstado()).isEqualTo(EstadoFase.EM_REVISAO);
        assertThat(fase.estadoApresentacao()).isEqualTo("Em Revisao - Em Atraso");
    }

    @Test
    @DisplayName("O prazo ultrapassado sem aprovacao marca a fase como atrasada")
    void prazoUltrapassadoSemAprovacao() {
        LocalDate hoje = LocalDate.of(2026, 8, 7);
        Fase pendente =
                Fase.builder()
                        .numero(1)
                        .nome("Tema e Protocolo")
                        .estado(EstadoFase.PENDENTE)
                        .dataLimite(hoje.minusDays(5))
                        .build();

        assertThat(pendente.prazoUltrapassado(hoje)).isTrue();
        assertThat(pendente.diasEmAtraso()).isPositive();
    }

    @Test
    @DisplayName("Uma fase aprovada nunca fica em atraso, mesmo com o prazo passado")
    void faseAprovadaNaoFicaEmAtraso() {
        LocalDate hoje = LocalDate.of(2026, 8, 7);
        Fase aprovada =
                Fase.builder()
                        .numero(1)
                        .nome("Tema e Protocolo")
                        .estado(EstadoFase.APROVADO)
                        .dataLimite(hoje.minusDays(30))
                        .build();

        assertThat(aprovada.prazoUltrapassado(hoje)).isFalse();
        assertThat(aprovada.diasRestantes()).isNull();
        assertThat(aprovada.diasEmAtraso()).isZero();
    }
}
