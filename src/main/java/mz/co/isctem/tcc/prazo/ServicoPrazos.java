package mz.co.isctem.tcc.prazo;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import mz.co.isctem.tcc.comum.ServicoAuditoria;
import mz.co.isctem.tcc.comum.excepcao.RecursoNaoEncontradoException;
import mz.co.isctem.tcc.comum.excepcao.RegraNegocioException;
import mz.co.isctem.tcc.dominio.enums.FaseTcc;
import mz.co.isctem.tcc.dominio.modelo.Fase;
import mz.co.isctem.tcc.dominio.modelo.Milestone;
import mz.co.isctem.tcc.dominio.modelo.Utilizador;
import mz.co.isctem.tcc.dominio.repositorio.FaseRepositorio;
import mz.co.isctem.tcc.dominio.repositorio.MilestoneRepositorio;
import mz.co.isctem.tcc.dominio.repositorio.UtilizadorRepositorio;
import mz.co.isctem.tcc.prazo.dto.AjustarPrazoFasePedido;
import mz.co.isctem.tcc.prazo.dto.MilestonePedido;
import mz.co.isctem.tcc.prazo.dto.MilestoneResposta;
import mz.co.isctem.tcc.seguranca.ContextoSeguranca;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF10: gestao de prazos e marcos do processo, da responsabilidade do Coordenador. */
@Service
@RequiredArgsConstructor
public class ServicoPrazos {

    private final MilestoneRepositorio milestones;
    private final FaseRepositorio fases;
    private final UtilizadorRepositorio utilizadores;
    private final ServicoAuditoria auditoria;
    private final ContextoSeguranca contexto;

    @Transactional(readOnly = true)
    public List<MilestoneResposta> listar() {
        return milestones.findAllByOrderByCursoAscAnoLectivoDescNumeroFaseAsc().stream()
                .map(ServicoPrazos::paraResposta)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MilestoneResposta> porCurso(String curso, int anoLectivo) {
        return milestones.findByCursoIgnoreCaseAndAnoLectivoOrderByNumeroFaseAsc(curso, anoLectivo).stream()
                .map(ServicoPrazos::paraResposta)
                .toList();
    }

    /** Cria ou actualiza o prazo de uma fase para um curso e ano lectivo. */
    @Transactional
    public MilestoneResposta definir(MilestonePedido pedido) {
        if (!pedido.intervaloValido()) {
            throw new RegraNegocioException("A data limite nao pode ser anterior a data de inicio.");
        }
        FaseTcc definicao = FaseTcc.porNumero(pedido.numeroFase());

        Milestone milestone =
                milestones
                        .findByCursoIgnoreCaseAndAnoLectivoAndNumeroFase(
                                pedido.curso().trim(), pedido.anoLectivo(), pedido.numeroFase())
                        .orElseGet(
                                () ->
                                        Milestone.builder()
                                                .curso(pedido.curso().trim())
                                                .anoLectivo(pedido.anoLectivo())
                                                .numeroFase(pedido.numeroFase())
                                                .criadoPor(actor())
                                                .build());

        milestone.setNome(definicao.nomeApresentacao());
        milestone.setDataInicio(pedido.dataInicio());
        milestone.setDataLimite(pedido.dataLimite());
        Milestone guardado = milestones.save(milestone);

        auditoria.registar(
                actor(),
                "DEFINIR_PRAZO",
                "Milestone",
                guardado.getId(),
                "%s / %d / fase %d: %s a %s"
                        .formatted(
                                guardado.getCurso(),
                                guardado.getAnoLectivo(),
                                guardado.getNumeroFase(),
                                guardado.getDataInicio(),
                                guardado.getDataLimite()));
        return paraResposta(guardado);
    }

    @Transactional
    public void remover(Long id) {
        Milestone milestone =
                milestones.findById(id).orElseThrow(() -> RecursoNaoEncontradoException.de("Milestone", id));
        milestones.delete(milestone);
        auditoria.registar(actor(), "REMOVER_PRAZO", "Milestone", id, null);
    }

    /**
     * Aplica o plano de prazos de um curso as fases ainda nao aprovadas dos TCC em curso.
     *
     * <p>As fases ja aprovadas ficam intactas: alterar retroactivamente o prazo de algo concluido
     * distorceria os indicadores de cumprimento.
     */
    @Transactional
    public int aplicarAosProcessos(String curso, int anoLectivo) {
        List<Milestone> plano =
                milestones.findByCursoIgnoreCaseAndAnoLectivoOrderByNumeroFaseAsc(curso, anoLectivo);
        if (plano.isEmpty()) {
            throw new RegraNegocioException(
                    "Nao existe plano de prazos definido para %s no ano lectivo %d.".formatted(curso, anoLectivo));
        }

        int actualizadas = 0;
        for (Milestone milestone : plano) {
            List<Fase> alvo = fases.porCursoEFaseNaoAprovadas(curso, milestone.getNumeroFase());
            for (Fase fase : alvo) {
                fase.setDataInicio(milestone.getDataInicio());
                fase.setDataLimite(milestone.getDataLimite());
                // O atraso e reavaliado pela verificacao diaria com base no novo prazo.
                fase.setEmAtraso(fase.prazoUltrapassado(LocalDate.now()));
                actualizadas++;
            }
        }
        auditoria.registar(
                actor(),
                "APLICAR_PRAZOS",
                "Milestone",
                null,
                "%s / %d: %d fases actualizadas".formatted(curso, anoLectivo, actualizadas));
        return actualizadas;
    }

    /** Ajuste individual do prazo de uma fase de um estudante. */
    @Transactional
    public void ajustarFase(Long faseId, AjustarPrazoFasePedido pedido) {
        Fase fase = fases.findById(faseId).orElseThrow(() -> RecursoNaoEncontradoException.de("Fase", faseId));
        if (pedido.dataInicio() != null && pedido.dataLimite().isBefore(pedido.dataInicio())) {
            throw new RegraNegocioException("A data limite nao pode ser anterior a data de inicio.");
        }
        if (pedido.dataInicio() != null) {
            fase.setDataInicio(pedido.dataInicio());
        }
        fase.setDataLimite(pedido.dataLimite());
        fase.setEmAtraso(fase.prazoUltrapassado(LocalDate.now()));

        auditoria.registar(
                actor(),
                "AJUSTAR_PRAZO_FASE",
                "Fase",
                faseId,
                "nova data limite=" + pedido.dataLimite());
    }

    private Utilizador actor() {
        return utilizadores.findById(contexto.idUtilizadorActual()).orElse(null);
    }

    private static MilestoneResposta paraResposta(Milestone milestone) {
        return new MilestoneResposta(
                milestone.getId(),
                milestone.getCurso(),
                milestone.getAnoLectivo(),
                milestone.getNumeroFase(),
                milestone.getNome(),
                milestone.getDataInicio(),
                milestone.getDataLimite(),
                ChronoUnit.DAYS.between(milestone.getDataInicio(), milestone.getDataLimite()));
    }
}
