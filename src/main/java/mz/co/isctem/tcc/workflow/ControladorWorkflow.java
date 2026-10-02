package mz.co.isctem.tcc.workflow;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mz.co.isctem.tcc.seguranca.ContextoSeguranca;
import mz.co.isctem.tcc.workflow.dto.DecidirSubmissaoPedido;
import mz.co.isctem.tcc.workflow.dto.TccResposta;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** RF05 e RF06: workflow do TCC, submissoes e decisoes do supervisor. */
@RestController
@RequestMapping("/api/tccs")
@RequiredArgsConstructor
public class ControladorWorkflow {

    private final ServicoWorkflow servico;
    private final ContextoSeguranca contexto;

    /** Processo do estudante autenticado. */
    @GetMapping("/meu")
    @PreAuthorize("hasRole('ESTUDANTE')")
    public TccResposta meuProcesso() {
        return servico.meuProcesso(contexto.idUtilizadorActual());
    }

    /** A visibilidade e validada em {@link PoliticaAcessoTcc} conforme o perfil (RNF06). */
    @GetMapping("/{tccId}")
    public TccResposta porId(@PathVariable Long tccId) {
        return servico.porId(tccId);
    }

    @PostMapping(path = "/{tccId}/fases/{numeroFase}/submissoes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ESTUDANTE')")
    public TccResposta submeter(
            @PathVariable Long tccId,
            @PathVariable int numeroFase,
            @RequestPart("ficheiro") MultipartFile ficheiro,
            @RequestParam(value = "comentario", required = false) String comentario) {
        return servico.submeter(tccId, numeroFase, ficheiro, comentario);
    }

    @PostMapping("/{tccId}/submissoes/{submissaoId}/decisao")
    @PreAuthorize("hasRole('DOCENTE')")
    public TccResposta decidir(
            @PathVariable Long tccId,
            @PathVariable Long submissaoId,
            @Valid @RequestBody DecidirSubmissaoPedido pedido) {
        return servico.decidir(tccId, submissaoId, pedido);
    }
}
