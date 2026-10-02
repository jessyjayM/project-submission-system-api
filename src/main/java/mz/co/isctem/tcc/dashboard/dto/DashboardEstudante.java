package mz.co.isctem.tcc.dashboard.dto;

import java.time.LocalDate;
import mz.co.isctem.tcc.workflow.dto.TccResposta;

/** RF11: o estudante visualiza apenas o seu processo. */
public record DashboardEstudante(
        TccResposta tcc,
        int percentagemConclusao,
        String faseActualNome,
        String estadoFaseActual,
        LocalDate dataLimiteFaseActual,
        Long diasRestantes,
        boolean emAtraso,
        long notificacoesNaoLidas,
        long mensagensNaoLidas,
        String supervisorNome) {}
