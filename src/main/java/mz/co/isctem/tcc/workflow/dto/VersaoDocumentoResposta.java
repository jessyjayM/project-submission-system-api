package mz.co.isctem.tcc.workflow.dto;

import java.time.Instant;
import mz.co.isctem.tcc.plagio.dto.RelatorioPlagioResposta;

/** Versao de documento no historico de uma submissao (RF06). */
public record VersaoDocumentoResposta(
        Long id,
        int numeroVersao,
        String nomeOriginal,
        String extensao,
        long tamanhoBytes,
        Instant dataUpload,
        RelatorioPlagioResposta relatorioPlagio) {}
