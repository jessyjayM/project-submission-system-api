package mz.co.isctem.tcc.plagio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import mz.co.isctem.tcc.dominio.enums.EstadoRelatorioPlagio;

/**
 * Relatorio apresentado ao supervisor (RF08): percentagem de similaridade, lista de documentos
 * semelhantes e data de geracao.
 */
public record RelatorioPlagioResposta(
        Long id,
        Long versaoDocumentoId,
        BigDecimal percentagemSimilaridade,
        EstadoRelatorioPlagio estado,
        int documentosComparados,
        String mensagem,
        boolean acimaDoLimiar,
        Instant dataGeracao,
        List<PlagioCorrespondenciaResposta> documentosSemelhantes) {}
