package mz.co.isctem.tcc.chat;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import mz.co.isctem.tcc.chat.dto.MensagemResposta;
import mz.co.isctem.tcc.comum.PaginaResposta;
import mz.co.isctem.tcc.documento.ServicoArmazenamento;
import mz.co.isctem.tcc.dominio.modelo.Mensagem;
import mz.co.isctem.tcc.seguranca.ContextoSeguranca;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** RF07: chat privado entre estudante e supervisor, com leitura pelo Coordenador. */
@RestController
@RequestMapping("/api/tccs/{tccId}/chat")
@RequiredArgsConstructor
public class ControladorChat {

    private final ServicoChat servico;
    private final ServicoArmazenamento armazenamento;
    private final ContextoSeguranca contexto;

    @GetMapping
    public PaginaResposta<MensagemResposta> historico(
            @PathVariable Long tccId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamanho) {
        return servico.listar(
                tccId, PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "enviadaEm")));
    }

    /** Polling: devolve apenas as mensagens posteriores a ultima ja conhecida pelo cliente. */
    @GetMapping("/novas")
    public List<MensagemResposta> novas(
            @PathVariable Long tccId, @RequestParam(required = false) Long desdeId) {
        return servico.novasDesde(tccId, desdeId);
    }

    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public MensagemResposta enviar(
            @PathVariable Long tccId,
            @RequestParam(value = "conteudo", required = false) String conteudo,
            @RequestPart(value = "anexo", required = false) MultipartFile anexo) {
        return servico.enviar(tccId, conteudo, anexo);
    }

    @PostMapping("/lidas")
    public Map<String, Integer> marcarComoLidas(@PathVariable Long tccId) {
        return Map.of("actualizadas", servico.marcarComoLidas(tccId, contexto.idUtilizadorActual()));
    }

    @GetMapping("/nao-lidas")
    public Map<String, Long> contarNaoLidas(@PathVariable Long tccId) {
        return Map.of("total", servico.contarNaoLidas(tccId, contexto.idUtilizadorActual()));
    }

    @GetMapping("/mensagens/{mensagemId}/anexo")
    public ResponseEntity<Resource> anexo(@PathVariable Long tccId, @PathVariable Long mensagemId) {
        Mensagem mensagem = servico.mensagemComAnexo(mensagemId);
        Resource recurso = armazenamento.carregar(mensagem.getAnexoCaminho());
        String nomeCodificado =
                URLEncoder.encode(mensagem.getAnexoNome(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"%s\"; filename*=UTF-8''%s"
                                .formatted(
                                        mensagem.getAnexoNome().replaceAll("[^\\x20-\\x7E]", "_"), nomeCodificado))
                .body(recurso);
    }
}
