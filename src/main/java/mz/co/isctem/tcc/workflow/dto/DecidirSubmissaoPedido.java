package mz.co.isctem.tcc.workflow.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mz.co.isctem.tcc.dominio.enums.DecisaoRevisao;

/**
 * Decisao do supervisor (RF05).
 *
 * <p>O comentario e obrigatorio quando se solicitam correccoes: o estudante tem de saber o que
 * corrigir antes de voltar a submeter.
 */
public record DecidirSubmissaoPedido(
        @NotNull(message = "A decisao e obrigatoria.") DecisaoRevisao decisao,
        @Size(max = 4000, message = "O comentario nao pode exceder 4000 caracteres.") String comentario) {

    public boolean comentarioObrigatorioEmFalta() {
        return decisao == DecisaoRevisao.CORRECAO_SOLICITADA
                && (comentario == null || comentario.isBlank());
    }
}
