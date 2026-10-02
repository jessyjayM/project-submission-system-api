package mz.co.isctem.tcc.workflow;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mz.co.isctem.tcc.comum.ServicoAuditoria;
import mz.co.isctem.tcc.comum.excepcao.RecursoNaoEncontradoException;
import mz.co.isctem.tcc.comum.excepcao.RegraNegocioException;
import mz.co.isctem.tcc.documento.ExtractorTexto;
import mz.co.isctem.tcc.documento.ServicoArmazenamento;
import mz.co.isctem.tcc.dominio.enums.DecisaoRevisao;
import mz.co.isctem.tcc.dominio.enums.EstadoFase;
import mz.co.isctem.tcc.dominio.enums.EstadoProcessoEstudante;
import mz.co.isctem.tcc.dominio.enums.EstadoSubmissao;
import mz.co.isctem.tcc.dominio.enums.EstadoTcc;
import mz.co.isctem.tcc.dominio.enums.FaseTcc;
import mz.co.isctem.tcc.dominio.enums.TipoNotificacao;
import mz.co.isctem.tcc.dominio.modelo.Estudante;
import mz.co.isctem.tcc.dominio.modelo.Fase;
import mz.co.isctem.tcc.dominio.modelo.Milestone;
import mz.co.isctem.tcc.dominio.modelo.RelatorioPlagio;
import mz.co.isctem.tcc.dominio.modelo.Revisao;
import mz.co.isctem.tcc.dominio.modelo.Submissao;
import mz.co.isctem.tcc.dominio.modelo.Tcc;
import mz.co.isctem.tcc.dominio.modelo.Utilizador;
import mz.co.isctem.tcc.dominio.modelo.VersaoDocumento;
import mz.co.isctem.tcc.dominio.repositorio.FaseRepositorio;
import mz.co.isctem.tcc.dominio.repositorio.MilestoneRepositorio;
import mz.co.isctem.tcc.dominio.repositorio.SubmissaoRepositorio;
import mz.co.isctem.tcc.dominio.repositorio.TccRepositorio;
import mz.co.isctem.tcc.dominio.repositorio.UtilizadorRepositorio;
import mz.co.isctem.tcc.dominio.repositorio.VersaoDocumentoRepositorio;
import mz.co.isctem.tcc.notificacao.ServicoNotificacao;
import mz.co.isctem.tcc.plagio.ServicoPlagio;
import mz.co.isctem.tcc.seguranca.UtilizadorAutenticado;
import mz.co.isctem.tcc.workflow.dto.DecidirSubmissaoPedido;
import mz.co.isctem.tcc.workflow.dto.FaseResposta;
import mz.co.isctem.tcc.workflow.dto.TccResposta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * RF05 e RF06: workflow do TCC, submissoes e versionamento de documentos.
 *
 * <p>Implementa a maquina de estados da seccao 5.4. O ciclo {@code PENDENTE -> EM_REVISAO ->
 * CORRECAO_SOLICITADA -> PENDENTE} repete-se sem limite; so a aprovacao faz avancar a fase, e a
 * fase seguinte permanece inacessivel ate la.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServicoWorkflow {

    private final TccRepositorio tccs;
    private final FaseRepositorio fases;
    private final SubmissaoRepositorio submissoes;
    private final VersaoDocumentoRepositorio versoes;
    private final MilestoneRepositorio milestones;
    private final UtilizadorRepositorio utilizadores;
    private final ServicoArmazenamento armazenamento;
    private final ExtractorTexto extractorTexto;
    private final ServicoPlagio servicoPlagio;
    private final ServicoNotificacao notificacoes;
    private final ServicoAuditoria auditoria;
    private final PoliticaAcessoTcc politica;
    private final MapeadorWorkflow mapeador;

    // -----------------------------------------------------------------
    // Arranque do processo
    // -----------------------------------------------------------------

    /**
     * Cria o TCC e as respectivas fases quando o estudante recebe supervisor (RF05: "Apos a
     * atribuicao do supervisor, o estudante podera iniciar o workflow").
     *
     * <p>Idempotente: chamar de novo devolve o TCC existente.
     */
    @Transactional
    public Tcc garantirTccIniciado(Estudante estudante) {
        return tccs.findByEstudanteId(estudante.getId())
                .orElseGet(() -> criarTcc(estudante));
    }

    private Tcc criarTcc(Estudante estudante) {
        Tcc tcc =
                Tcc.builder()
                        .estudante(estudante)
                        .curso(estudante.getCurso())
                        .area(estudante.getArea())
                        .dataInicio(LocalDate.now())
                        .faseActual(1)
                        .estadoGeral(EstadoTcc.EM_CURSO)
                        .build();

        // A Fase 5 (Prontidao do Sistema) so existe para cursos da area de Informatica.
        List<Milestone> prazos =
                milestones.findByCursoIgnoreCaseAndAnoLectivoOrderByNumeroFaseAsc(
                        estudante.getCurso(), LocalDate.now().getYear());

        List<Fase> criadas = new ArrayList<>();
        for (FaseTcc definicao : FaseTcc.aplicaveis(estudante.getArea())) {
            Milestone prazo =
                    prazos.stream()
                            .filter(m -> m.getNumeroFase() == definicao.numero())
                            .findFirst()
                            .orElse(null);
            criadas.add(
                    Fase.builder()
                            .tcc(tcc)
                            .numero(definicao.numero())
                            .nome(definicao.nomeApresentacao())
                            .estado(EstadoFase.PENDENTE)
                            .dataInicio(prazo != null ? prazo.getDataInicio() : null)
                            .dataLimite(prazo != null ? prazo.getDataLimite() : null)
                            .build());
        }
        // A primeira fase arranca de imediato se nao houver plano de prazos definido.
        criadas.getFirst().setDataInicio(
                criadas.getFirst().getDataInicio() == null ? LocalDate.now() : criadas.getFirst().getDataInicio());
        tcc.getFases().addAll(criadas);

        prazos.stream()
                .filter(m -> m.getNumeroFase() == FaseTcc.ultimaFase(estudante.getArea()))
                .findFirst()
                .ifPresent(ultimo -> tcc.setDataFimPrevista(ultimo.getDataLimite()));

        Tcc guardado = tccs.save(tcc);
        estudante.setEstadoProcesso(EstadoProcessoEstudante.EM_CURSO);
        auditoria.registarDoSistema(
                "INICIAR_WORKFLOW",
                "Tcc",
                guardado.getId(),
                "estudante=%s, fases=%d".formatted(estudante.getUtilizador().getNome(), criadas.size()));
        return guardado;
    }

    // -----------------------------------------------------------------
    // Consulta
    // -----------------------------------------------------------------

    @Transactional(readOnly = true)
    public TccResposta porId(Long tccId) {
        Tcc tcc = carregar(tccId);
        politica.exigirLeitura(tcc);
        return montarResposta(tcc);
    }

    /** Processo do estudante autenticado (RF11: "o estudante visualiza apenas o seu workflow"). */
    @Transactional(readOnly = true)
    public TccResposta meuProcesso(Long utilizadorId) {
        Tcc tcc =
                tccs.findByEstudanteUtilizadorId(utilizadorId)
                        .orElseThrow(
                                () ->
                                        new RecursoNaoEncontradoException(
                                                "Ainda nao tem um processo de TCC. O workflow arranca depois de o "
                                                        + "Coordenador lhe atribuir um supervisor."));
        return montarResposta(tcc);
    }

    private TccResposta montarResposta(Tcc tcc) {
        List<FaseResposta> respostasFases =
                tcc.getFases().stream()
                        .sorted((a, b) -> Integer.compare(a.getNumero(), b.getNumero()))
                        .map(
                                fase ->
                                        mapeador.paraResposta(
                                                fase, tcc.getFaseActual(), submissoes.findByFaseIdOrderByNumeroDesc(fase.getId())))
                        .toList();
        return mapeador.paraResposta(tcc, respostasFases);
    }

    // -----------------------------------------------------------------
    // Submissao (RF06)
    // -----------------------------------------------------------------

    /**
     * Submete um documento na fase indicada.
     *
     * <p>Cada submissao gera automaticamente uma nova versao (a anterior nunca e substituida) e
     * desencadeia a verificacao de plagio antes de a submissao ficar visivel ao supervisor.
     */
    @Transactional
    public TccResposta submeter(Long tccId, int numeroFase, MultipartFile ficheiro, String comentario) {
        Tcc tcc = carregar(tccId);
        UtilizadorAutenticado actual = politica.exigirEstudanteDono(tcc);
        Fase fase = faseDo(tcc, numeroFase);

        if (numeroFase > tcc.getFaseActual()) {
            throw new RegraNegocioException(
                    "A Fase %d ainda esta bloqueada. Conclua e obtenha aprovacao da Fase %d primeiro."
                            .formatted(numeroFase, tcc.getFaseActual()));
        }
        if (numeroFase < tcc.getFaseActual() || fase.getEstado().terminal()) {
            throw new RegraNegocioException("A Fase %d ja foi aprovada e nao aceita novas submissoes.".formatted(numeroFase));
        }
        if (!fase.getEstado().permiteSubmissao()) {
            throw new RegraNegocioException(
                    "Ja existe uma submissao a aguardar avaliacao do supervisor nesta fase.");
        }

        Utilizador autor =
                utilizadores
                        .findById(actual.id())
                        .orElseThrow(() -> RecursoNaoEncontradoException.de("Utilizador", actual.id()));

        Submissao submissao =
                submissoes.save(
                        Submissao.builder()
                                .fase(fase)
                                .autor(autor)
                                .numero(submissoes.ultimoNumero(fase.getId()) + 1)
                                .comentarioEstudante(comentario)
                                .estado(EstadoSubmissao.EM_REVISAO)
                                .dataSubmissao(Instant.now())
                                .build());

        VersaoDocumento versao = guardarVersao(tcc, fase, submissao, ficheiro);

        // RF08: a analise corre antes de a submissao ser dada como disponivel ao supervisor.
        RelatorioPlagio relatorio = servicoPlagio.analisar(versao);

        fase.setEstado(EstadoFase.EM_REVISAO);

        auditoria.registar(
                autor,
                "SUBMETER_DOCUMENTO",
                "Submissao",
                submissao.getId(),
                "fase=%d, versao=%d, similaridade=%s%%"
                        .formatted(numeroFase, versao.getNumeroVersao(), relatorio.getPercentagemSimilaridade()));

        notificarSupervisor(
                tcc,
                TipoNotificacao.NOVA_SUBMISSAO,
                "Nova submissao de " + autor.getNome(),
                "%s submeteu documentos na Fase %d (%s). Similaridade detectada: %s%%."
                        .formatted(
                                autor.getNome(), numeroFase, fase.getNome(), relatorio.getPercentagemSimilaridade()),
                "/supervisao/tccs/" + tcc.getId());

        if (servicoPlagio.acimaDoLimiar(relatorio)) {
            notificarSupervisor(
                    tcc,
                    TipoNotificacao.PLAGIO_ELEVADO,
                    "Similaridade elevada detectada",
                    "A submissao de %s na Fase %d apresenta %s%% de similaridade com documentos existentes."
                            .formatted(autor.getNome(), numeroFase, relatorio.getPercentagemSimilaridade()),
                    "/supervisao/tccs/" + tcc.getId());
        }
        return montarResposta(tcc);
    }

    private VersaoDocumento guardarVersao(Tcc tcc, Fase fase, Submissao submissao, MultipartFile ficheiro) {
        String subPasta = "tcc-%d/fase-%d/submissao-%d".formatted(tcc.getId(), fase.getNumero(), submissao.getId());
        ServicoArmazenamento.FicheiroGuardado guardado = armazenamento.guardar(ficheiro, subPasta);

        String texto =
                extractorTexto.extrair(
                        armazenamento.caminhoAbsoluto(guardado.caminhoRelativo()), guardado.extensao());

        VersaoDocumento versao =
                versoes.save(
                        VersaoDocumento.builder()
                                .submissao(submissao)
                                .numeroVersao(versoes.ultimoNumeroVersao(submissao.getId()) + 1)
                                .nomeOriginal(guardado.nomeOriginal())
                                .caminho(guardado.caminhoRelativo())
                                .tipoConteudo(guardado.tipoConteudo())
                                .extensao(guardado.extensao())
                                .tamanhoBytes(guardado.tamanho())
                                .hashSha256(guardado.hashSha256())
                                .textoExtraido(texto)
                                .build());
        submissao.getVersoes().add(versao);
        return versao;
    }

    // -----------------------------------------------------------------
    // Decisao do supervisor (RF05)
    // -----------------------------------------------------------------

    /**
     * Aprova ou devolve a submissao em revisao.
     *
     * <p>Criterio de aceitacao: com a aprovacao, o sistema avanca automaticamente o estudante para a
     * fase seguinte e notifica ambas as partes.
     */
    @Transactional
    public TccResposta decidir(Long tccId, Long submissaoId, DecidirSubmissaoPedido pedido) {
        if (pedido.comentarioObrigatorioEmFalta()) {
            throw new RegraNegocioException(
                    "Indique as correccoes a efectuar para que o estudante saiba o que rever.");
        }
        Tcc tcc = carregar(tccId);
        UtilizadorAutenticado actual = politica.exigirSupervisorDoTcc(tcc);

        Submissao submissao =
                submissoes
                        .findById(submissaoId)
                        .filter(s -> s.getFase().getTcc().getId().equals(tccId))
                        .orElseThrow(() -> RecursoNaoEncontradoException.de("Submissao", submissaoId));

        Fase fase = submissao.getFase();
        if (!fase.getEstado().permiteDecisao() || submissao.getEstado() != EstadoSubmissao.EM_REVISAO) {
            throw new RegraNegocioException("Esta submissao ja foi avaliada.");
        }

        Utilizador revisor =
                utilizadores
                        .findById(actual.id())
                        .orElseThrow(() -> RecursoNaoEncontradoException.de("Utilizador", actual.id()));

        submissao.getRevisoes()
                .add(
                        Revisao.builder()
                                .submissao(submissao)
                                .revisor(revisor)
                                .decisao(pedido.decisao())
                                .comentario(pedido.comentario())
                                .dataRevisao(Instant.now())
                                .build());
        submissao.setDataDecisao(Instant.now());

        if (pedido.decisao() == DecisaoRevisao.APROVADO) {
            aprovarFase(tcc, fase, submissao, revisor);
        } else {
            solicitarCorreccao(tcc, fase, submissao, revisor, pedido.comentario());
        }
        return montarResposta(tcc);
    }

    private void aprovarFase(Tcc tcc, Fase fase, Submissao submissao, Utilizador revisor) {
        submissao.setEstado(EstadoSubmissao.APROVADO);
        fase.setEstado(EstadoFase.APROVADO);
        fase.setDataAprovacao(Instant.now());
        // O atraso deixa de fazer sentido depois da aprovacao.
        fase.setEmAtraso(false);

        // Fase 1: o tema aprovado passa a ser o titulo do TCC.
        if (fase.getNumero() == FaseTcc.TEMA_E_PROTOCOLO.numero()
                && tcc.getTitulo() == null
                && submissao.getComentarioEstudante() != null) {
            tcc.setTitulo(truncar(submissao.getComentarioEstudante(), 300));
        }

        auditoria.registar(
                revisor, "APROVAR_SUBMISSAO", "Submissao", submissao.getId(), "fase=" + fase.getNumero());

        Utilizador estudante = tcc.getEstudante().getUtilizador();
        int ultima = tcc.ultimaFase();

        if (fase.getNumero() >= ultima) {
            // Aprovada a ultima fase aplicavel, o estudante fica apto para defesa.
            tcc.setEstadoGeral(EstadoTcc.APTO_PARA_DEFESA);
            tcc.getEstudante().setEstadoProcesso(EstadoProcessoEstudante.APTO_PARA_DEFESA);
            notificacoes.criar(
                    estudante,
                    TipoNotificacao.APROVACAO,
                    "Apto para defesa",
                    "Concluiu todas as fases do TCC com aprovacao. Esta apto para a defesa.",
                    "/meu-tcc");
            notificarSupervisor(
                    tcc,
                    TipoNotificacao.APROVACAO,
                    "Orientando apto para defesa",
                    "%s concluiu todas as fases do TCC.".formatted(estudante.getNome()),
                    "/supervisao/tccs/" + tcc.getId());
            return;
        }

        // Avanco automatico para a fase seguinte.
        int seguinte = fase.getNumero() + 1;
        tcc.setFaseActual(seguinte);
        tcc.fase(seguinte)
                .ifPresent(
                        proxima -> {
                            if (proxima.getDataInicio() == null) {
                                proxima.setDataInicio(LocalDate.now());
                            }
                        });

        String nomeSeguinte =
                tcc.fase(seguinte).map(Fase::getNome).orElse("Fase " + seguinte);
        notificacoes.criar(
                estudante,
                TipoNotificacao.MUDANCA_FASE,
                "Fase %d aprovada".formatted(fase.getNumero()),
                "A Fase %d (%s) foi aprovada. Avancou para a Fase %d: %s."
                        .formatted(fase.getNumero(), fase.getNome(), seguinte, nomeSeguinte),
                "/meu-tcc");
        notificarSupervisor(
                tcc,
                TipoNotificacao.MUDANCA_FASE,
                "Fase aprovada",
                "%s avancou para a Fase %d: %s.".formatted(estudante.getNome(), seguinte, nomeSeguinte),
                "/supervisao/tccs/" + tcc.getId());
    }

    private void solicitarCorreccao(
            Tcc tcc, Fase fase, Submissao submissao, Utilizador revisor, String comentario) {
        submissao.setEstado(EstadoSubmissao.CORRECAO_SOLICITADA);
        // Volta a permitir submissoes: o ciclo repete-se ate haver aprovacao.
        fase.setEstado(EstadoFase.CORRECAO_SOLICITADA);

        auditoria.registar(
                revisor,
                "SOLICITAR_CORRECCAO",
                "Submissao",
                submissao.getId(),
                "fase=" + fase.getNumero());

        notificacoes.criar(
                tcc.getEstudante().getUtilizador(),
                TipoNotificacao.CORRECAO_SOLICITADA,
                "Correccoes solicitadas na Fase %d".formatted(fase.getNumero()),
                "O supervisor devolveu a sua submissao com recomendacoes: " + truncar(comentario, 800),
                "/meu-tcc");
    }

    // -----------------------------------------------------------------
    // Apoio
    // -----------------------------------------------------------------

    private void notificarSupervisor(
            Tcc tcc, TipoNotificacao tipo, String titulo, String conteudo, String ligacao) {
        if (tcc.getEstudante().temSupervisor()) {
            notificacoes.criar(
                    tcc.getEstudante().getSupervisor().getUtilizador(), tipo, titulo, conteudo, ligacao);
        }
    }

    private Fase faseDo(Tcc tcc, int numero) {
        return fases.findByTccIdAndNumero(tcc.getId(), numero)
                .orElseThrow(
                        () ->
                                new RecursoNaoEncontradoException(
                                        "A Fase %d nao existe neste processo. Cursos fora da area de Informatica "
                                                .formatted(numero)
                                                + "concluem o workflow na Fase 4."));
    }

    Tcc carregar(Long tccId) {
        return tccs.findById(tccId).orElseThrow(() -> RecursoNaoEncontradoException.de("TCC", tccId));
    }

    private static String truncar(String texto, int maximo) {
        if (texto == null) {
            return "";
        }
        return texto.length() <= maximo ? texto : texto.substring(0, maximo - 3) + "...";
    }
}
