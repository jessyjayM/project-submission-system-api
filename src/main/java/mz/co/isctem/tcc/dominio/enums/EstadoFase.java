package mz.co.isctem.tcc.dominio.enums;

/**
 * Maquina de estados do workflow (seccao 5.4 do levantamento de requisitos).
 *
 * <p>O ciclo {@code PENDENTE -> EM_REVISAO -> CORRECAO_SOLICITADA -> PENDENTE} pode repetir-se um
 * numero ilimitado de vezes; a unica saida e {@link #APROVADO}, que despoleta o avanco de fase.
 *
 * <p>"Em Atraso" nao figura aqui de proposito: e um estado transversal, indicado em paralelo
 * atraves da flag {@code emAtraso} da fase, e nunca substitui o estado do fluxo.
 */
public enum EstadoFase {
    /** Aguarda a primeira submissao do estudante (ou uma nova, apos correccao). */
    PENDENTE,
    /** Submissao efectuada, aguarda avaliacao do supervisor. */
    EM_REVISAO,
    /** O supervisor devolveu com recomendacoes; o estudante pode voltar a submeter. */
    CORRECAO_SOLICITADA,
    /** O supervisor aprovou; a fase seguinte e desbloqueada. */
    APROVADO;

    /** Estados a partir dos quais o estudante pode submeter (ou re-submeter) documentos. */
    public boolean permiteSubmissao() {
        return this == PENDENTE || this == CORRECAO_SOLICITADA;
    }

    /** Apenas uma submissao em revisao pode ser avaliada pelo supervisor. */
    public boolean permiteDecisao() {
        return this == EM_REVISAO;
    }

    public boolean terminal() {
        return this == APROVADO;
    }
}
