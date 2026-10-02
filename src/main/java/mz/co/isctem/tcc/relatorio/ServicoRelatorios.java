package mz.co.isctem.tcc.relatorio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import mz.co.isctem.tcc.config.PropriedadesIsctem;
import mz.co.isctem.tcc.dashboard.ServicoDashboard;
import mz.co.isctem.tcc.dominio.enums.DecisaoRevisao;
import mz.co.isctem.tcc.dominio.enums.EstadoSubmissao;
import mz.co.isctem.tcc.dominio.enums.EstadoTcc;
import mz.co.isctem.tcc.dominio.enums.FaseTcc;
import mz.co.isctem.tcc.dominio.repositorio.EstudanteRepositorio;
import mz.co.isctem.tcc.dominio.repositorio.FaseRepositorio;
import mz.co.isctem.tcc.dominio.repositorio.RelatorioPlagioRepositorio;
import mz.co.isctem.tcc.dominio.repositorio.RevisaoRepositorio;
import mz.co.isctem.tcc.dominio.repositorio.SubmissaoRepositorio;
import mz.co.isctem.tcc.dominio.repositorio.TccRepositorio;
import mz.co.isctem.tcc.relatorio.dto.RelatoriosResposta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF12: emissao dos relatorios institucionais, da competencia do Coordenador. */
@Service
@RequiredArgsConstructor
public class ServicoRelatorios {

    private static final ZoneId FUSO = ZoneId.of("Africa/Maputo");

    private final EstudanteRepositorio estudantes;
    private final TccRepositorio tccs;
    private final FaseRepositorio fases;
    private final SubmissaoRepositorio submissoes;
    private final RevisaoRepositorio revisoes;
    private final RelatorioPlagioRepositorio relatoriosPlagio;
    private final ServicoDashboard servicoDashboard;
    private final PropriedadesIsctem propriedades;

    @Transactional(readOnly = true)
    public RelatoriosResposta gerar() {
        return new RelatoriosResposta(
                Instant.now(),
                progresso(),
                desempenhoSupervisores(),
                cumprimentoPrazos(),
                taxaAprovacao(),
                tempoMedioPorFase(),
                historicoSubmissoes(),
                estatisticasPlagio());
    }

    private RelatoriosResposta.ProgressoGeral progresso() {
        long total = estudantes.count();
        long semSupervisor = estudantes.countBySupervisorIsNull();
        return new RelatoriosResposta.ProgressoGeral(
                total,
                total - semSupervisor,
                semSupervisor,
                tccs.countByEstadoGeral(EstadoTcc.EM_CURSO),
                tccs.countByEstadoGeral(EstadoTcc.APTO_PARA_DEFESA),
                tccs.countByEstadoGeral(EstadoTcc.CONCLUIDO));
    }

    private List<RelatoriosResposta.DesempenhoSupervisor> desempenhoSupervisores() {
        Map<Long, Long> orientandos = new HashMap<>();
        estudantes
                .contarPorSupervisor()
                .forEach(linha -> orientandos.put((Long) linha[0], ((Number) linha[2]).longValue()));

        List<RelatoriosResposta.DesempenhoSupervisor> resultado = new ArrayList<>();
        for (Object[] linha : revisoes.desempenhoPorSupervisor()) {
            Long supervisorId = (Long) linha[0];
            long totalRevisoes = ((Number) linha[2]).longValue();
            long aprovacoes = ((Number) linha[3]).longValue();
            resultado.add(
                    new RelatoriosResposta.DesempenhoSupervisor(
                            supervisorId,
                            (String) linha[1],
                            orientandos.getOrDefault(supervisorId, 0L),
                            totalRevisoes,
                            aprovacoes,
                            totalRevisoes - aprovacoes,
                            servicoDashboard.tempoMedioRespostaHoras(supervisorId)));
        }
        return resultado;
    }

    private RelatoriosResposta.CumprimentoPrazos cumprimentoPrazos() {
        long total = fases.count();
        long emAtraso = fases.countByEmAtrasoTrue();
        long noPrazo = total - emAtraso;
        double percentagem = total == 0 ? 100.0 : arredondar(noPrazo * 100.0 / total);
        return new RelatoriosResposta.CumprimentoPrazos(noPrazo, emAtraso, percentagem);
    }

    private RelatoriosResposta.TaxaAprovacao taxaAprovacao() {
        long aprovadas = revisoes.countByDecisao(DecisaoRevisao.APROVADO);
        long devolvidas = revisoes.countByDecisao(DecisaoRevisao.CORRECAO_SOLICITADA);
        long avaliadas = aprovadas + devolvidas;
        double percentagem = avaliadas == 0 ? 0.0 : arredondar(aprovadas * 100.0 / avaliadas);
        return new RelatoriosResposta.TaxaAprovacao(avaliadas, aprovadas, devolvidas, percentagem);
    }

    /** Media, por fase, dos dias decorridos entre o inicio e a aprovacao. */
    private List<RelatoriosResposta.TempoMedioFase> tempoMedioPorFase() {
        Map<Integer, List<Long>> duracoesPorFase = new HashMap<>();
        for (Object[] linha : fases.duracoesDasFasesConcluidas()) {
            int numero = ((Number) linha[0]).intValue();
            LocalDate inicio = (LocalDate) linha[1];
            Instant aprovacao = (Instant) linha[2];
            long dias = ChronoUnit.DAYS.between(inicio, aprovacao.atZone(FUSO).toLocalDate());
            duracoesPorFase.computeIfAbsent(numero, k -> new ArrayList<>()).add(Math.max(0, dias));
        }
        return duracoesPorFase.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(
                        entrada -> {
                            List<Long> valores = entrada.getValue();
                            double media = valores.stream().mapToLong(Long::longValue).average().orElse(0);
                            return new RelatoriosResposta.TempoMedioFase(
                                    entrada.getKey(),
                                    FaseTcc.porNumero(entrada.getKey()).nomeApresentacao(),
                                    arredondar(media),
                                    valores.size());
                        })
                .toList();
    }

    private RelatoriosResposta.HistoricoSubmissoes historicoSubmissoes() {
        return new RelatoriosResposta.HistoricoSubmissoes(
                submissoes.count(),
                submissoes.countByEstado(EstadoSubmissao.EM_REVISAO),
                submissoes.countByEstado(EstadoSubmissao.APROVADO),
                submissoes.countByEstado(EstadoSubmissao.CORRECAO_SOLICITADA));
    }

    private RelatoriosResposta.EstatisticasPlagio estatisticasPlagio() {
        BigDecimal limiar =
                propriedades.plagio().limiarAlerta().multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);

        List<Object[]> linhas = relatoriosPlagio.estatisticas();
        if (linhas.isEmpty() || linhas.getFirst() == null) {
            return new RelatoriosResposta.EstatisticasPlagio(
                    0, BigDecimal.ZERO, BigDecimal.ZERO, 0, limiar);
        }
        Object[] estatisticas = linhas.getFirst();
        return new RelatoriosResposta.EstatisticasPlagio(
                ((Number) estatisticas[0]).longValue(),
                paraPercentagem(estatisticas[1]),
                paraPercentagem(estatisticas[2]),
                relatoriosPlagio.contarAcimaDe(limiar),
                limiar);
    }

    private static BigDecimal paraPercentagem(Object valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal decimal =
                valor instanceof BigDecimal bd ? bd : BigDecimal.valueOf(((Number) valor).doubleValue());
        return decimal.setScale(2, RoundingMode.HALF_UP);
    }

    private static double arredondar(double valor) {
        return Math.round(valor * 10) / 10.0;
    }
}
