package mz.co.isctem.tcc.dashboard.dto;

import java.time.Instant;
import java.util.List;
import mz.co.isctem.tcc.supervisao.dto.EstudanteResumo;

/** RF11: o supervisor visualiza apenas os seus orientandos. */
public record DashboardSupervisor(
        long totalOrientandos,
        long orientandosEmAtraso,
        long submissoesPorAvaliar,
        long mensagensNaoLidas,
        List<EstudanteResumo> orientandos,
        List<PendenciaResposta> pendencias) {

    /** Submissao a aguardar decisao do supervisor. */
    public record PendenciaResposta(
            Long tccId,
            Long submissaoId,
            String estudanteNome,
            int numeroFase,
            String faseNome,
            Instant dataSubmissao,
            long diasAEspera) {}
}
