package mz.co.isctem.tcc.prazo;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mz.co.isctem.tcc.comum.ServicoAuditoria;
import mz.co.isctem.tcc.dominio.enums.PerfilUtilizador;
import mz.co.isctem.tcc.dominio.enums.TipoNotificacao;
import mz.co.isctem.tcc.dominio.modelo.Fase;
import mz.co.isctem.tcc.dominio.modelo.Utilizador;
import mz.co.isctem.tcc.dominio.repositorio.FaseRepositorio;
import mz.co.isctem.tcc.dominio.repositorio.UtilizadorRepositorio;
import mz.co.isctem.tcc.notificacao.ServicoNotificacao;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF10: verificacao diaria de prazos.
 *
 * <p>Criterio de aceitacao: "Dado que a data limite de uma fase foi ultrapassada sem aprovacao,
 * quando o sistema executa a verificacao diaria de prazos, entao o estado do estudante deve ser
 * automaticamente marcado como Em Atraso e uma notificacao deve ser enviada ao supervisor e ao
 * coordenador."
 *
 * <p>Corre as 02:00 (hora de Maputo) e tambem avisa com antecedencia as fases cujo prazo termina
 * nos proximos dias.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VerificadorPrazos {

    /** Antecedencia, em dias, do aviso de prazo a aproximar-se. */
    private static final int DIAS_DE_AVISO = 3;

    private final FaseRepositorio fases;
    private final UtilizadorRepositorio utilizadores;
    private final ServicoNotificacao notificacoes;
    private final ServicoAuditoria auditoria;

    @Scheduled(cron = "0 0 2 * * *", zone = "Africa/Maputo")
    public void verificacaoDiaria() {
        int atrasos = marcarAtrasos();
        int avisos = avisarPrazosProximos();
        log.info("Verificacao diaria de prazos concluida: {} atraso(s), {} aviso(s).", atrasos, avisos);
    }

    /** Marca como Em Atraso as fases cujo prazo passou sem aprovacao e notifica os envolvidos. */
    @Transactional
    public int marcarAtrasos() {
        LocalDate hoje = LocalDate.now();
        List<Fase> atrasadas = fases.encontrarPrazosUltrapassados(hoje);
        if (atrasadas.isEmpty()) {
            return 0;
        }
        List<Utilizador> coordenadores = utilizadores.findByTipo(PerfilUtilizador.COORDENADOR);

        for (Fase fase : atrasadas) {
            // Estado transversal: acumula com o estado do fluxo, nunca o substitui (seccao 5.4).
            fase.setEmAtraso(true);

            var tcc = fase.getTcc();
            var estudante = tcc.getEstudante();
            long dias = fase.diasEmAtraso();
            String resumo =
                    "%s (%s) esta em atraso na Fase %d - %s. Prazo terminou a %s (%d dia(s))."
                            .formatted(
                                    estudante.getUtilizador().getNome(),
                                    estudante.getUtilizador().getCodigo(),
                                    fase.getNumero(),
                                    fase.getNome(),
                                    fase.getDataLimite(),
                                    dias);

            notificacoes.criar(
                    estudante.getUtilizador(),
                    TipoNotificacao.ATRASO,
                    "Fase %d em atraso".formatted(fase.getNumero()),
                    "O prazo da Fase %d (%s) terminou a %s sem aprovacao."
                            .formatted(fase.getNumero(), fase.getNome(), fase.getDataLimite()),
                    "/meu-tcc");

            if (estudante.temSupervisor()) {
                notificacoes.criar(
                        estudante.getSupervisor().getUtilizador(),
                        TipoNotificacao.ATRASO,
                        "Orientando em atraso",
                        resumo,
                        "/supervisao/tccs/" + tcc.getId());
            }
            notificacoes.criarParaTodos(
                    coordenadores, TipoNotificacao.ATRASO, "Estudante em atraso", resumo, "/coordenacao/estudantes");

            auditoria.registarDoSistema(
                    "MARCAR_EM_ATRASO", "Fase", fase.getId(), "dias em atraso=" + dias);
        }
        return atrasadas.size();
    }

    /** Avisa estudante e supervisor das fases cujo prazo termina nos proximos dias (RF09). */
    @Transactional
    public int avisarPrazosProximos() {
        LocalDate hoje = LocalDate.now();
        List<Fase> proximas = fases.encontrarPrazosAExpirar(hoje, hoje.plusDays(DIAS_DE_AVISO));

        for (Fase fase : proximas) {
            var estudante = fase.getTcc().getEstudante();
            Long restantes = fase.diasRestantes();
            notificacoes.criar(
                    estudante.getUtilizador(),
                    TipoNotificacao.PRAZO_A_APROXIMAR,
                    "Prazo a aproximar-se",
                    "Faltam %d dia(s) para o prazo da Fase %d - %s (%s)."
                            .formatted(
                                    restantes == null ? 0 : restantes,
                                    fase.getNumero(),
                                    fase.getNome(),
                                    fase.getDataLimite()),
                    "/meu-tcc");
            if (estudante.temSupervisor()) {
                notificacoes.criar(
                        estudante.getSupervisor().getUtilizador(),
                        TipoNotificacao.PRAZO_A_APROXIMAR,
                        "Prazo de orientando a aproximar-se",
                        "%s tem %d dia(s) ate ao prazo da Fase %d."
                                .formatted(
                                        estudante.getUtilizador().getNome(),
                                        restantes == null ? 0 : restantes,
                                        fase.getNumero()),
                        "/supervisao/tccs/" + fase.getTcc().getId());
            }
        }
        return proximas.size();
    }
}
