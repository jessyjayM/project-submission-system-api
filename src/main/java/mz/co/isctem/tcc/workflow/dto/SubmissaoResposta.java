package mz.co.isctem.tcc.workflow.dto;

import java.time.Instant;
import java.util.List;
import mz.co.isctem.tcc.dominio.enums.EstadoSubmissao;

/** Submissao do estudante com o respectivo historico de versoes e revisoes (RF05, RF06). */
public record SubmissaoResposta(
        Long id,
        int numero,
        EstadoSubmissao estado,
        String comentarioEstudante,
        String autorNome,
        Instant dataSubmissao,
        Instant dataDecisao,
        List<VersaoDocumentoResposta> versoes,
        List<RevisaoResposta> revisoes) {}
