package mz.co.isctem.tcc.notificacao;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import mz.co.isctem.tcc.comum.PaginaResposta;
import mz.co.isctem.tcc.notificacao.dto.NotificacaoResposta;
import mz.co.isctem.tcc.seguranca.ContextoSeguranca;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** RF09: consulta das notificacoes do proprio utilizador. */
@RestController
@RequestMapping("/api/notificacoes")
@RequiredArgsConstructor
public class ControladorNotificacao {

    private final ServicoNotificacao servico;
    private final ContextoSeguranca contexto;

    @GetMapping
    public PaginaResposta<NotificacaoResposta> listar(
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanho) {
        return servico.listar(contexto.idUtilizadorActual(), PageRequest.of(pagina, tamanho));
    }

    @GetMapping("/nao-lidas")
    public Map<String, Long> contarNaoLidas() {
        return Map.of("total", servico.contarNaoLidas(contexto.idUtilizadorActual()));
    }

    @PostMapping("/{id}/lida")
    public ResponseEntity<Void> marcarComoLida(@PathVariable Long id) {
        servico.marcarComoLida(id, contexto.idUtilizadorActual());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/lidas")
    public Map<String, Integer> marcarTodasComoLidas() {
        return Map.of("actualizadas", servico.marcarTodasComoLidas(contexto.idUtilizadorActual()));
    }
}
