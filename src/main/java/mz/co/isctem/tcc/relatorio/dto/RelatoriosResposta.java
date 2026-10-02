package mz.co.isctem.tcc.relatorio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * RF12: relatorios de acompanhamento dos Trabalhos de Conclusao de Curso.
 *
 * <p>Reune num unico documento os sete eixos pedidos: progresso dos estudantes, desempenho dos
 * supervisores, cumprimento dos prazos, taxa de aprovacao, tempo medio por fase, historico de
 * submissoes e estatisticas de plagio.
 */
public record RelatoriosResposta(
        Instant geradoEm,
        ProgressoGeral progresso,
        List<DesempenhoSupervisor> desempenhoSupervisores,
        CumprimentoPrazos cumprimentoPrazos,
        TaxaAprovacao taxaAprovacao,
        List<TempoMedioFase> tempoMedioPorFase,
        HistoricoSubmissoes historicoSubmissoes,
        EstatisticasPlagio estatisticasPlagio) {

    public record ProgressoGeral(
            long totalEstudantes,
            long comSupervisor,
            long semSupervisor,
            long emCurso,
            long aptosParaDefesa,
            long concluidos) {}

    public record DesempenhoSupervisor(
            Long supervisorId,
            String nome,
            long orientandos,
            long revisoesEfectuadas,
            long aprovacoes,
            long correccoesSolicitadas,
            Double tempoMedioRespostaHoras) {}

    public record CumprimentoPrazos(long fasesNoPrazo, long fasesEmAtraso, double percentagemNoPrazo) {}

    public record TaxaAprovacao(
            long submissoesAvaliadas,
            long aprovadas,
            long devolvidasParaCorreccao,
            double percentagemAprovacao) {}

    public record TempoMedioFase(int numeroFase, String nome, double diasMedios, long amostras) {}

    public record HistoricoSubmissoes(
            long totalSubmissoes, long emRevisao, long aprovadas, long comCorreccaoSolicitada) {}

    public record EstatisticasPlagio(
            long documentosAnalisados,
            BigDecimal similaridadeMedia,
            BigDecimal similaridadeMaxima,
            long acimaDoLimiar,
            BigDecimal limiarConfigurado) {}
}
