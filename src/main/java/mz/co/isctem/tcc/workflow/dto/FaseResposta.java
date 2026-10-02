package mz.co.isctem.tcc.workflow.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import mz.co.isctem.tcc.dominio.enums.EstadoFase;

/**
 * Fase do workflow (seccao 5.4).
 *
 * <p>{@code bloqueada} traduz a regra "nao e possivel avancar para a fase seguinte sem aprovacao da
 * fase actual": o frontend usa-a para impedir o acesso, e o backend repete a verificacao na
 * submissao.
 */
public record FaseResposta(
        Long id,
        int numero,
        String nome,
        EstadoFase estado,
        boolean emAtraso,
        String estadoApresentacao,
        boolean bloqueada,
        boolean permiteSubmissao,
        LocalDate dataInicio,
        LocalDate dataLimite,
        Instant dataAprovacao,
        Long diasRestantes,
        long diasEmAtraso,
        List<SubmissaoResposta> submissoes) {}
