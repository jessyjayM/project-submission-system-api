package mz.co.isctem.tcc.plagio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mz.co.isctem.tcc.comum.excepcao.RecursoNaoEncontradoException;
import mz.co.isctem.tcc.config.PropriedadesIsctem;
import mz.co.isctem.tcc.dominio.enums.EstadoRelatorioPlagio;
import mz.co.isctem.tcc.dominio.modelo.PlagioCorrespondencia;
import mz.co.isctem.tcc.dominio.modelo.RelatorioPlagio;
import mz.co.isctem.tcc.dominio.modelo.VersaoDocumento;
import mz.co.isctem.tcc.dominio.repositorio.RelatorioPlagioRepositorio;
import mz.co.isctem.tcc.dominio.repositorio.VersaoDocumentoRepositorio;
import mz.co.isctem.tcc.plagio.dto.PlagioCorrespondenciaResposta;
import mz.co.isctem.tcc.plagio.dto.RelatorioPlagioResposta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF08: deteccao de plagio.
 *
 * <p>Toda a submissao desencadeia automaticamente uma verificacao. O alcance e o corpus interno do
 * proprio sistema (as monografias ja existentes na base de dados), sem integracao com servicos
 * externos como o Turnitin ou o Copyleaks, que implicariam custos de licenciamento.
 *
 * <p>Criterio de aceitacao: o relatorio e gerado antes de a submissao ficar disponivel ao
 * supervisor, pelo que a analise corre de forma sincrona dentro da transaccao da submissao.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServicoPlagio {

    private final VersaoDocumentoRepositorio versoes;
    private final RelatorioPlagioRepositorio relatorios;
    private final AnalisadorSimilaridade analisador;
    private final PropriedadesIsctem propriedades;

    /** Analisa a versao recem-submetida e devolve o relatorio ja persistido. */
    @Transactional
    public RelatorioPlagio analisar(VersaoDocumento versao) {
        PropriedadesIsctem.Plagio config = propriedades.plagio();
        Long tccId = versao.getSubmissao().getFase().getTcc().getId();

        if (versao.getTextoExtraido() == null || versao.getTextoExtraido().isBlank()) {
            return relatorios.save(
                    RelatorioPlagio.builder()
                            .versaoDocumento(versao)
                            .estado(EstadoRelatorioPlagio.FALHOU)
                            .percentagemSimilaridade(BigDecimal.ZERO)
                            .mensagem(
                                    "Nao foi possivel extrair texto do documento (ex.: PDF digitalizado). "
                                            + "A verificacao de similaridade nao e conclusiva.")
                            .build());
        }

        List<VersaoDocumento> corpus = versoes.corpusExcluindoTcc(tccId);
        if (corpus.isEmpty()) {
            return relatorios.save(
                    RelatorioPlagio.builder()
                            .versaoDocumento(versao)
                            .estado(EstadoRelatorioPlagio.SEM_CORPUS)
                            .percentagemSimilaridade(BigDecimal.ZERO)
                            .documentosComparados(0)
                            .mensagem("Ainda nao existem outros documentos na base de dados para comparacao.")
                            .build());
        }

        Map<Long, VersaoDocumento> porId =
                corpus.stream().collect(Collectors.toMap(VersaoDocumento::getId, Function.identity()));
        List<AnalisadorSimilaridade.Semelhanca> semelhancas =
                analisador.comparar(
                        versao.getTextoExtraido(),
                        corpus.stream()
                                .map(v -> new AnalisadorSimilaridade.DocumentoCorpus(v.getId(), v.getTextoExtraido()))
                                .toList(),
                        config.minTermos());

        RelatorioPlagio relatorio =
                RelatorioPlagio.builder()
                        .versaoDocumento(versao)
                        .estado(EstadoRelatorioPlagio.CONCLUIDO)
                        .documentosComparados(corpus.size())
                        .percentagemSimilaridade(
                                semelhancas.isEmpty()
                                        ? BigDecimal.ZERO
                                        : percentagem(semelhancas.getFirst().similaridade()))
                        .build();

        List<PlagioCorrespondencia> correspondencias = new ArrayList<>();
        semelhancas.stream()
                .limit(config.maxDocumentosSemelhantes())
                .forEach(
                        semelhanca -> {
                            VersaoDocumento semelhante = porId.get(semelhanca.id());
                            correspondencias.add(
                                    PlagioCorrespondencia.builder()
                                            .relatorio(relatorio)
                                            .versaoSemelhante(semelhante)
                                            .percentagemSimilaridade(percentagem(semelhanca.similaridade()))
                                            .descricao(descrever(semelhante))
                                            .build());
                        });
        relatorio.getCorrespondencias().addAll(correspondencias);
        return relatorios.save(relatorio);
    }

    @Transactional(readOnly = true)
    public RelatorioPlagioResposta porVersao(Long versaoId) {
        RelatorioPlagio relatorio =
                relatorios
                        .findByVersaoDocumentoId(versaoId)
                        .orElseThrow(
                                () -> RecursoNaoEncontradoException.de("Relatorio de plagio da versao", versaoId));
        return paraResposta(relatorio);
    }

    public RelatorioPlagioResposta paraResposta(RelatorioPlagio relatorio) {
        if (relatorio == null) {
            return null;
        }
        return new RelatorioPlagioResposta(
                relatorio.getId(),
                relatorio.getVersaoDocumento().getId(),
                relatorio.getPercentagemSimilaridade(),
                relatorio.getEstado(),
                relatorio.getDocumentosComparados(),
                relatorio.getMensagem(),
                acimaDoLimiar(relatorio),
                relatorio.getDataGeracao(),
                relatorio.getCorrespondencias().stream()
                        .map(
                                c ->
                                        new PlagioCorrespondenciaResposta(
                                                c.getVersaoSemelhante().getId(),
                                                c.getDescricao(),
                                                c.getPercentagemSimilaridade()))
                        .toList());
    }

    /** Acima deste valor a submissao e assinalada ao supervisor com destaque. */
    public boolean acimaDoLimiar(RelatorioPlagio relatorio) {
        BigDecimal limiar = propriedades.plagio().limiarAlerta().multiply(BigDecimal.valueOf(100));
        return relatorio.getEstado() == EstadoRelatorioPlagio.CONCLUIDO
                && relatorio.getPercentagemSimilaridade().compareTo(limiar) >= 0;
    }

    private static BigDecimal percentagem(double similaridade) {
        return BigDecimal.valueOf(similaridade * 100).setScale(2, RoundingMode.HALF_UP);
    }

    private static String descrever(VersaoDocumento versao) {
        var fase = versao.getSubmissao().getFase();
        var estudante = fase.getTcc().getEstudante();
        return "%s (%s) - Fase %d: %s, versao %d"
                .formatted(
                        estudante.getUtilizador().getNome(),
                        estudante.getUtilizador().getCodigo(),
                        fase.getNumero(),
                        fase.getNome(),
                        versao.getNumeroVersao());
    }
}
