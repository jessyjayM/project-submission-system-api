package mz.co.isctem.tcc.dashboard.dto;

import java.util.List;

/** RF11: indicadores do Coordenador. */
public record DashboardCoordenador(
        long totalEstudantes,
        long estudantesSemSupervisor,
        long estudantesEmAtraso,
        long estudantesAptosParaDefesa,
        long submissoesPendentesDeAvaliacao,
        Double tempoMedioRespostaSupervisoresHoras,
        Double tempoMedioSubmissaoEstudantesDias,
        List<ContagemPorFase> estudantesPorFase,
        List<ContagemPorSupervisor> estudantesPorSupervisor) {

    public record ContagemPorFase(int numeroFase, String nome, long total) {}

    public record ContagemPorSupervisor(Long supervisorId, String nome, long total) {}
}
