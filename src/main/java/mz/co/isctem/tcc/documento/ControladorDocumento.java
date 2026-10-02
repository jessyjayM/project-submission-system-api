package mz.co.isctem.tcc.documento;

import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import lombok.RequiredArgsConstructor;
import mz.co.isctem.tcc.comum.excepcao.RecursoNaoEncontradoException;
import mz.co.isctem.tcc.dominio.modelo.VersaoDocumento;
import mz.co.isctem.tcc.dominio.repositorio.VersaoDocumentoRepositorio;
import mz.co.isctem.tcc.plagio.ServicoPlagio;
import mz.co.isctem.tcc.plagio.dto.RelatorioPlagioResposta;
import mz.co.isctem.tcc.workflow.PoliticaAcessoTcc;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RF06: download e visualizacao das versoes de documento.
 *
 * <p>O acesso e sempre validado contra o TCC a que a versao pertence (RNF06), pelo que conhecer o
 * id de uma versao nao chega para a descarregar.
 */
@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
public class ControladorDocumento {

    private final VersaoDocumentoRepositorio versoes;
    private final ServicoArmazenamento armazenamento;
    private final ServicoPlagio servicoPlagio;
    private final PoliticaAcessoTcc politica;

    @GetMapping("/versoes/{versaoId}")
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> descarregar(@PathVariable Long versaoId) {
        VersaoDocumento versao = carregarComPermissao(versaoId);
        Resource recurso = armazenamento.carregar(versao.getCaminho());

        String nomeCodificado = URLEncoder.encode(versao.getNomeOriginal(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(versao.getTipoConteudo()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"%s\"; filename*=UTF-8''%s"
                                .formatted(versao.getNomeOriginal().replaceAll("[^\\x20-\\x7E]", "_"), nomeCodificado))
                .body(recurso);
    }

    /** RF08: relatorio de similaridade associado a versao. */
    @GetMapping("/versoes/{versaoId}/plagio")
    @Transactional(readOnly = true)
    public RelatorioPlagioResposta relatorioPlagio(@PathVariable Long versaoId) {
        carregarComPermissao(versaoId);
        return servicoPlagio.porVersao(versaoId);
    }

    private VersaoDocumento carregarComPermissao(Long versaoId) {
        VersaoDocumento versao =
                versoes
                        .findById(versaoId)
                        .orElseThrow(() -> RecursoNaoEncontradoException.de("Versao de documento", versaoId));
        politica.exigirLeitura(versao.getSubmissao().getFase().getTcc());
        return versao;
    }
}
