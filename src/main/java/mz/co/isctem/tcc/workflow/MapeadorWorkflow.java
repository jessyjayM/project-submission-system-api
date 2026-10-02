package mz.co.isctem.tcc.workflow;

import java.util.Comparator;
import java.util.List;
import mz.co.isctem.tcc.dominio.enums.EstadoTcc;
import mz.co.isctem.tcc.dominio.modelo.Fase;
import mz.co.isctem.tcc.dominio.modelo.Revisao;
import mz.co.isctem.tcc.dominio.modelo.Submissao;
import mz.co.isctem.tcc.dominio.modelo.Tcc;
import mz.co.isctem.tcc.dominio.modelo.VersaoDocumento;
import mz.co.isctem.tcc.plagio.ServicoPlagio;
import mz.co.isctem.tcc.workflow.dto.FaseResposta;
import mz.co.isctem.tcc.workflow.dto.RevisaoResposta;
import mz.co.isctem.tcc.workflow.dto.SubmissaoResposta;
import mz.co.isctem.tcc.workflow.dto.TccResposta;
import mz.co.isctem.tcc.workflow.dto.VersaoDocumentoResposta;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Conversao do agregado de workflow para os DTOs da API.
 *
 * <p>As partes lineares sao geradas pelo MapStruct; os campos derivados da maquina de estados
 * (bloqueio de fases, dias em atraso, percentagem de conclusao) sao calculados nos metodos
 * concretos, onde a regra fica visivel.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public abstract class MapeadorWorkflow {

    @Autowired protected ServicoPlagio servicoPlagio;

    @Mapping(target = "revisorNome", source = "revisor.nome")
    public abstract RevisaoResposta paraResposta(Revisao revisao);

    public VersaoDocumentoResposta paraResposta(VersaoDocumento versao) {
        return new VersaoDocumentoResposta(
                versao.getId(),
                versao.getNumeroVersao(),
                versao.getNomeOriginal(),
                versao.getExtensao(),
                versao.getTamanhoBytes(),
                versao.getDataUpload(),
                servicoPlagio.paraResposta(versao.getRelatorioPlagio()));
    }

    public SubmissaoResposta paraResposta(Submissao submissao) {
        return new SubmissaoResposta(
                submissao.getId(),
                submissao.getNumero(),
                submissao.getEstado(),
                submissao.getComentarioEstudante(),
                submissao.getAutor().getNome(),
                submissao.getDataSubmissao(),
                submissao.getDataDecisao(),
                submissao.getVersoes().stream()
                        .sorted(Comparator.comparingInt(VersaoDocumento::getNumeroVersao).reversed())
                        .map(this::paraResposta)
                        .toList(),
                submissao.getRevisoes().stream().map(this::paraResposta).toList());
    }

    /**
     * @param faseActual numero da fase corrente do TCC, usado para determinar o bloqueio
     */
    public FaseResposta paraResposta(Fase fase, int faseActual, List<Submissao> submissoes) {
        // Regra de negocio: nao e possivel aceder a fase seguinte sem aprovacao da fase actual.
        boolean bloqueada = fase.getNumero() > faseActual;
        return new FaseResposta(
                fase.getId(),
                fase.getNumero(),
                fase.getNome(),
                fase.getEstado(),
                fase.isEmAtraso(),
                fase.estadoApresentacao(),
                bloqueada,
                !bloqueada && fase.getEstado().permiteSubmissao(),
                fase.getDataInicio(),
                fase.getDataLimite(),
                fase.getDataAprovacao(),
                fase.diasRestantes(),
                fase.diasEmAtraso(),
                submissoes.stream()
                        .sorted(Comparator.comparingInt(Submissao::getNumero).reversed())
                        .map(this::paraResposta)
                        .toList());
    }

    public TccResposta paraResposta(Tcc tcc, List<FaseResposta> fases) {
        var estudante = tcc.getEstudante();
        var supervisor = estudante.getSupervisor();
        return new TccResposta(
                tcc.getId(),
                estudante.getId(),
                estudante.getUtilizador().getNome(),
                estudante.getUtilizador().getCodigo(),
                tcc.getCurso(),
                tcc.getArea(),
                tcc.getTitulo(),
                tcc.getFaseActual(),
                tcc.ultimaFase(),
                tcc.getEstadoGeral(),
                tcc.percentagemConclusao(),
                supervisor == null ? null : supervisor.getId(),
                supervisor == null ? null : supervisor.getUtilizador().getNome(),
                tcc.getDataInicio(),
                tcc.getDataFimPrevista(),
                tcc.getEstadoGeral() == EstadoTcc.APTO_PARA_DEFESA,
                fases);
    }
}
