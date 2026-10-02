package mz.co.isctem.tcc.dashboard;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import mz.co.isctem.tcc.comum.excepcao.RecursoNaoEncontradoException;
import mz.co.isctem.tcc.dashboard.dto.DashboardCoordenador;
import mz.co.isctem.tcc.dashboard.dto.DashboardEstudante;
import mz.co.isctem.tcc.dashboard.dto.DashboardSupervisor;
import mz.co.isctem.tcc.dominio.enums.EstadoSubmissao;
import mz.co.isctem.tcc.dominio.enums.EstadoTcc;
import mz.co.isctem.tcc.dominio.enums.FaseTcc;
import mz.co.isctem.tcc.dominio.modelo.Docente;
import mz.co.isctem.tcc.dominio.modelo.Fase;
import mz.co.isctem.tcc.dominio.modelo.Submissao;
import mz.co.isctem.tcc.dominio.modelo.Tcc;
import mz.co.isctem.tcc.dominio.repositorio.DocenteRepositorio;
import mz.co.isctem.tcc.dominio.repositorio.EstudanteRepositorio;
import mz.co.isctem.tcc.dominio.repositorio.FaseRepositorio;
import mz.co.isctem.tcc.dominio.repositorio.MensagemRepositorio;
import mz.co.isctem.tcc.dominio.repositorio.NotificacaoRepositorio;
import mz.co.isctem.tcc.dominio.repositorio.SubmissaoRepositorio;
import mz.co.isctem.tcc.dominio.repositorio.TccRepositorio;
import mz.co.isctem.tcc.supervisao.ServicoSupervisao;
import mz.co.isctem.tcc.workflow.ServicoWorkflow;
import mz.co.isctem.tcc.workflow.dto.TccResposta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF11: dashboards com indicadores do processo, diferenciados por perfil.
 *
 * <p>Cada metodo devolve apenas o que o respectivo perfil pode ver (RNF06).
 */
@Service
@RequiredArgsConstructor
public class ServicoDashboard {

    private final EstudanteRepositorio estudantes;
    private final DocenteRepositorio docentes;
    private final TccRepositorio tccs;
    private final FaseRepositorio fases;
    private final SubmissaoRepositorio submissoes;
    private final MensagemRepositorio mensagens;
    private final NotificacaoRepositorio notificacoes;
    private final ServicoSupervisao servicoSupervisao;
    private final ServicoWorkflow servicoWorkflow;

    @Transactional(readOnly = true)
    public DashboardCoordenador doCoordenador() {
        List<DashboardCoordenador.ContagemPorFase> porFase =
                tccs.contarPorFase().stream()
                        .map(
                                linha -> {
                                    int numero = ((Number) linha[0]).intValue();
                                    return new DashboardCoordenador.ContagemPorFase(
                                            numero,
                                            FaseTcc.porNumero(numero).nomeApresentacao(),
                                            ((Number) linha[1]).longValue());
                                })
                        .sorted((a, b) -> Integer.compare(a.numeroFase(), b.numeroFase()))
                        .toList();

        List<DashboardCoordenador.ContagemPorSupervisor> porSupervisor =
                estudantes.contarPorSupervisor().stream()
                        .map(
                                linha ->
                                        new DashboardCoordenador.ContagemPorSupervisor(
                                                (Long) linha[0], (String) linha[1], ((Number) linha[2]).longValue()))
                        .toList();

        return new DashboardCoordenador(
                estudantes.count(),
                estudantes.countBySupervisorIsNull(),
                tccs.contarEmAtraso(),
                tccs.countByEstadoGeral(EstadoTcc.APTO_PARA_DEFESA),
                submissoes.countByEstado(EstadoSubmissao.EM_REVISAO),
                tempoMedioRespostaHoras(null),
                tempoMedioSubmissaoDias(),
                porFase,
                porSupervisor);
    }

    @Transactional(readOnly = true)
    public DashboardSupervisor doSupervisor(Long utilizadorId) {
        Docente docente =
                docentes
                        .findByUtilizadorId(utilizadorId)
                        .orElseThrow(
                                () -> new RecursoNaoEncontradoException("Nao existe um docente associado a esta conta."));

        var orientandos = servicoSupervisao.meusOrientandos(utilizadorId);
        List<Submissao> pendentes = submissoes.pendentesDoSupervisor(docente.getId());

        List<DashboardSupervisor.PendenciaResposta> pendencias =
                pendentes.stream()
                        .map(
                                submissao -> {
                                    Fase fase = submissao.getFase();
                                    return new DashboardSupervisor.PendenciaResposta(
                                            fase.getTcc().getId(),
                                            submissao.getId(),
                                            fase.getTcc().getEstudante().getUtilizador().getNome(),
                                            fase.getNumero(),
                                            fase.getNome(),
                                            submissao.getDataSubmissao(),
                                            Duration.between(submissao.getDataSubmissao(), Instant.now()).toDays());
                                })
                        .toList();

        long naoLidas =
                tccs.findByEstudanteSupervisorId(docente.getId()).stream()
                        .mapToLong(tcc -> mensagens.contarNaoLidas(tcc.getId(), utilizadorId))
                        .sum();

        return new DashboardSupervisor(
                orientandos.size(),
                orientandos.stream().filter(o -> o.emAtraso()).count(),
                pendencias.size(),
                naoLidas,
                orientandos,
                pendencias);
    }

    @Transactional(readOnly = true)
    public DashboardEstudante doEstudante(Long utilizadorId) {
        TccResposta tcc = servicoWorkflow.meuProcesso(utilizadorId);
        Tcc entidade =
                tccs.findByEstudanteUtilizadorId(utilizadorId)
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Processo de TCC nao encontrado."));
        Fase corrente = entidade.faseCorrente().orElse(null);

        return new DashboardEstudante(
                tcc,
                tcc.percentagemConclusao(),
                corrente == null ? null : corrente.getNome(),
                corrente == null ? null : corrente.estadoApresentacao(),
                corrente == null ? null : corrente.getDataLimite(),
                corrente == null ? null : corrente.diasRestantes(),
                corrente != null && corrente.isEmAtraso(),
                notificacoes.countByDestinatarioIdAndLidaFalse(utilizadorId),
                mensagens.contarNaoLidas(entidade.getId(), utilizadorId),
                tcc.supervisorNome());
    }

    /**
     * RF11: tempo medio de resposta dos supervisores, em horas.
     *
     * @param supervisorId limita a um supervisor; {@code null} abrange toda a instituicao
     */
    @Transactional(readOnly = true)
    public Double tempoMedioRespostaHoras(Long supervisorId) {
        List<Object[]> tempos = submissoes.temposDeResposta(supervisorId);
        if (tempos.isEmpty()) {
            return null;
        }
        double total =
                tempos.stream()
                        .mapToDouble(
                                linha ->
                                        Duration.between((Instant) linha[0], (Instant) linha[1]).toMinutes() / 60.0)
                        .sum();
        return Math.round(total / tempos.size() * 10) / 10.0;
    }

    /**
     * RF11: tempo medio de resposta dos estudantes, em dias — quanto demoram entre o inicio da fase
     * e a submissao efectiva.
     */
    Double tempoMedioSubmissaoDias() {
        List<Object[]> duracoes = fases.duracoesDasFasesConcluidas();
        if (duracoes.isEmpty()) {
            return null;
        }
        double total =
                duracoes.stream()
                        .mapToDouble(
                                linha -> {
                                    java.time.LocalDate inicio = (java.time.LocalDate) linha[1];
                                    Instant aprovacao = (Instant) linha[2];
                                    return java.time.temporal.ChronoUnit.DAYS.between(
                                            inicio, aprovacao.atZone(java.time.ZoneId.of("Africa/Maputo")).toLocalDate());
                                })
                        .sum();
        return Math.round(total / duracoes.size() * 10) / 10.0;
    }
}
