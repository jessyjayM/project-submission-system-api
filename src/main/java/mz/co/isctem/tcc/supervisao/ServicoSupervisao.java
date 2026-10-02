package mz.co.isctem.tcc.supervisao;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mz.co.isctem.tcc.comum.PaginaResposta;
import mz.co.isctem.tcc.comum.ServicoAuditoria;
import mz.co.isctem.tcc.comum.excepcao.RecursoNaoEncontradoException;
import mz.co.isctem.tcc.comum.excepcao.RegraNegocioException;
import mz.co.isctem.tcc.dominio.enums.TipoNotificacao;
import mz.co.isctem.tcc.dominio.modelo.AtribuicaoSupervisor;
import mz.co.isctem.tcc.dominio.modelo.Docente;
import mz.co.isctem.tcc.dominio.modelo.Estudante;
import mz.co.isctem.tcc.dominio.modelo.Fase;
import mz.co.isctem.tcc.dominio.modelo.Tcc;
import mz.co.isctem.tcc.dominio.modelo.Utilizador;
import mz.co.isctem.tcc.dominio.repositorio.AtribuicaoSupervisorRepositorio;
import mz.co.isctem.tcc.dominio.repositorio.DocenteRepositorio;
import mz.co.isctem.tcc.dominio.repositorio.EstudanteRepositorio;
import mz.co.isctem.tcc.dominio.repositorio.TccRepositorio;
import mz.co.isctem.tcc.dominio.repositorio.UtilizadorRepositorio;
import mz.co.isctem.tcc.notificacao.ServicoNotificacao;
import mz.co.isctem.tcc.seguranca.ContextoSeguranca;
import mz.co.isctem.tcc.supervisao.dto.AtribuicaoHistoricoResposta;
import mz.co.isctem.tcc.supervisao.dto.AtribuirSupervisorPedido;
import mz.co.isctem.tcc.supervisao.dto.DocenteResumo;
import mz.co.isctem.tcc.supervisao.dto.EstudanteResumo;
import mz.co.isctem.tcc.workflow.ServicoWorkflow;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF04: gestao de supervisores pelo Coordenador.
 *
 * <p>Regra de negocio: cada estudante possui apenas um supervisor activo num dado momento; um
 * supervisor pode orientar varios estudantes em simultaneo. A atribuicao inicial arranca o workflow
 * do TCC.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServicoSupervisao {

    private final EstudanteRepositorio estudantes;
    private final DocenteRepositorio docentes;
    private final AtribuicaoSupervisorRepositorio atribuicoes;
    private final TccRepositorio tccs;
    private final UtilizadorRepositorio utilizadores;
    private final ServicoWorkflow workflow;
    private final ServicoNotificacao notificacoes;
    private final ServicoAuditoria auditoria;
    private final ContextoSeguranca contexto;

    @Transactional(readOnly = true)
    public PaginaResposta<EstudanteResumo> listarEstudantes(
            String termo, Long supervisorId, boolean semSupervisor, Pageable pagina) {
        return PaginaResposta.de(
                estudantes.pesquisar(
                        termo == null || termo.isBlank() ? null : termo.trim(),
                        supervisorId,
                        semSupervisor,
                        pagina),
                this::paraResumo);
    }

    @Transactional(readOnly = true)
    public List<DocenteResumo> listarDocentes() {
        Map<Long, Long> orientandosPorDocente =
                estudantes.contarPorSupervisor().stream()
                        .collect(
                                Collectors.toMap(
                                        linha -> (Long) linha[0], linha -> (Long) linha[2], (a, b) -> a));

        return docentes.listarTodosComUtilizador().stream()
                .map(
                        docente ->
                                new DocenteResumo(
                                        docente.getId(),
                                        docente.getUtilizador().getId(),
                                        docente.getUtilizador().getNome(),
                                        docente.getUtilizador().getEmail(),
                                        docente.getDepartamento(),
                                        orientandosPorDocente.getOrDefault(docente.getId(), 0L)))
                .toList();
    }

    /**
     * Atribui ou substitui o supervisor de um estudante.
     *
     * <p>Fecha a atribuicao anterior (mantendo-a no historico), abre a nova e, se for a primeira,
     * arranca o workflow do TCC.
     */
    @Transactional
    public EstudanteResumo atribuir(AtribuirSupervisorPedido pedido) {
        Estudante estudante =
                estudantes
                        .findById(pedido.estudanteId())
                        .orElseThrow(() -> RecursoNaoEncontradoException.de("Estudante", pedido.estudanteId()));
        Docente docente =
                docentes
                        .findById(pedido.docenteId())
                        .orElseThrow(() -> RecursoNaoEncontradoException.de("Docente", pedido.docenteId()));

        if (!docente.getUtilizador().activo()) {
            throw new RegraNegocioException("O docente seleccionado tem a conta desactivada.");
        }

        Docente anterior = estudante.getSupervisor();
        if (anterior != null && anterior.getId().equals(docente.getId())) {
            throw new RegraNegocioException("Este docente ja e o supervisor activo do estudante.");
        }
        if (anterior != null && (pedido.motivo() == null || pedido.motivo().isBlank())) {
            throw new RegraNegocioException(
                    "Indique o motivo da substituicao do supervisor para registo no historico.");
        }

        // Encerra a atribuicao em vigor: so pode existir uma activa por estudante.
        //
        // O flush e indispensavel. A chave da tabela e IDENTITY, pelo que o save() seguinte executa
        // o INSERT de imediato; sem forcar aqui o UPDATE do `fim`, este so sairia no commit e o
        // indice unico parcial `ux_atribuicao_activa` veria duas linhas activas para o mesmo
        // estudante, rejeitando a substituicao.
        atribuicoes
                .findByEstudanteIdAndFimIsNull(estudante.getId())
                .ifPresent(
                        activa -> {
                            activa.setFim(Instant.now());
                            atribuicoes.saveAndFlush(activa);
                        });

        Utilizador coordenador = actor();
        atribuicoes.save(
                AtribuicaoSupervisor.builder()
                        .estudante(estudante)
                        .docente(docente)
                        .atribuidoPor(coordenador)
                        .inicio(Instant.now())
                        .motivo(pedido.motivo())
                        .build());
        estudante.setSupervisor(docente);

        // RF05: o workflow so arranca depois de existir supervisor.
        Tcc tcc = workflow.garantirTccIniciado(estudante);

        auditoria.registar(
                coordenador,
                anterior == null ? "ATRIBUIR_SUPERVISOR" : "SUBSTITUIR_SUPERVISOR",
                "Estudante",
                estudante.getId(),
                "supervisor %s -> %s"
                        .formatted(
                                anterior == null ? "(nenhum)" : anterior.getUtilizador().getNome(),
                                docente.getUtilizador().getNome()));

        notificacoes.criar(
                estudante.getUtilizador(),
                TipoNotificacao.ALTERACAO_SUPERVISOR,
                anterior == null ? "Supervisor atribuido" : "Supervisor alterado",
                "O seu supervisor de TCC passa a ser %s.".formatted(docente.getUtilizador().getNome()),
                "/meu-tcc");
        notificacoes.criar(
                docente.getUtilizador(),
                TipoNotificacao.ALTERACAO_SUPERVISOR,
                "Novo orientando atribuido",
                "Passou a orientar %s (%s)."
                        .formatted(estudante.getUtilizador().getNome(), estudante.getUtilizador().getCodigo()),
                "/supervisao/tccs/" + tcc.getId());
        if (anterior != null) {
            notificacoes.criar(
                    anterior.getUtilizador(),
                    TipoNotificacao.ALTERACAO_SUPERVISOR,
                    "Orientacao terminada",
                    "Deixou de orientar %s. Motivo: %s"
                            .formatted(estudante.getUtilizador().getNome(), pedido.motivo()),
                    "/supervisao");
        }
        return paraResumo(estudante);
    }

    @Transactional(readOnly = true)
    public List<AtribuicaoHistoricoResposta> historico(Long estudanteId) {
        return atribuicoes.findByEstudanteIdOrderByInicioDesc(estudanteId).stream()
                .map(
                        atribuicao ->
                                new AtribuicaoHistoricoResposta(
                                        atribuicao.getId(),
                                        atribuicao.getDocente().getId(),
                                        atribuicao.getDocente().getUtilizador().getNome(),
                                        atribuicao.getAtribuidoPor().getNome(),
                                        atribuicao.getInicio(),
                                        atribuicao.getFim(),
                                        atribuicao.getFim() == null,
                                        atribuicao.getMotivo()))
                .toList();
    }

    /** RF11: orientandos do supervisor autenticado. */
    @Transactional(readOnly = true)
    public List<EstudanteResumo> meusOrientandos(Long utilizadorDocenteId) {
        Docente docente =
                docentes
                        .findByUtilizadorId(utilizadorDocenteId)
                        .orElseThrow(
                                () -> new RecursoNaoEncontradoException("Nao existe um docente associado a esta conta."));
        return estudantes.findBySupervisorId(docente.getId()).stream().map(this::paraResumo).toList();
    }

    EstudanteResumo paraResumo(Estudante estudante) {
        Tcc tcc = tccs.findByEstudanteId(estudante.getId()).orElse(null);
        Fase faseCorrente = tcc == null ? null : tcc.faseCorrente().orElse(null);
        Docente supervisor = estudante.getSupervisor();

        return new EstudanteResumo(
                estudante.getId(),
                estudante.getUtilizador().getId(),
                estudante.getUtilizador().getNome(),
                estudante.getUtilizador().getCodigo(),
                estudante.getCurso(),
                estudante.getArea(),
                estudante.getEstadoProcesso(),
                supervisor == null ? null : supervisor.getId(),
                supervisor == null ? null : supervisor.getUtilizador().getNome(),
                tcc == null ? null : tcc.getId(),
                tcc == null ? null : tcc.getFaseActual(),
                faseCorrente == null ? null : faseCorrente.getNome(),
                faseCorrente == null ? null : faseCorrente.estadoApresentacao(),
                faseCorrente != null && faseCorrente.isEmAtraso(),
                faseCorrente == null ? null : faseCorrente.getDataLimite(),
                faseCorrente == null ? null : faseCorrente.diasRestantes(),
                tcc == null ? 0 : tcc.percentagemConclusao());
    }

    private Utilizador actor() {
        Long id = contexto.idUtilizadorActual();
        return utilizadores
                .findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Utilizador", id));
    }
}
