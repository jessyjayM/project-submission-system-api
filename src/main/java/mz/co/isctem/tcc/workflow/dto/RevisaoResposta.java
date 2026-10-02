package mz.co.isctem.tcc.workflow.dto;

import java.time.Instant;
import mz.co.isctem.tcc.dominio.enums.DecisaoRevisao;

/** Decisao do supervisor sobre uma submissao (RF05). */
public record RevisaoResposta(
        Long id, DecisaoRevisao decisao, String comentario, String revisorNome, Instant dataRevisao) {}
