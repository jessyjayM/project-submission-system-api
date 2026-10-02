package mz.co.isctem.tcc.prazo;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import mz.co.isctem.tcc.prazo.dto.AjustarPrazoFasePedido;
import mz.co.isctem.tcc.prazo.dto.MilestonePedido;
import mz.co.isctem.tcc.prazo.dto.MilestoneResposta;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** RF10: gestao de prazos e milestones. Area exclusiva do Coordenador. */
@RestController
@RequestMapping("/api/prazos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COORDENADOR')")
public class ControladorPrazos {

    private final ServicoPrazos servico;
    private final VerificadorPrazos verificador;

    @GetMapping
    public List<MilestoneResposta> listar() {
        return servico.listar();
    }

    @GetMapping("/curso")
    public List<MilestoneResposta> porCurso(
            @RequestParam String curso, @RequestParam int anoLectivo) {
        return servico.porCurso(curso, anoLectivo);
    }

    @PostMapping
    public MilestoneResposta definir(@Valid @RequestBody MilestonePedido pedido) {
        return servico.definir(pedido);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        servico.remover(id);
        return ResponseEntity.noContent().build();
    }

    /** Propaga o plano de prazos do curso as fases ainda nao aprovadas. */
    @PostMapping("/aplicar")
    public Map<String, Integer> aplicar(@RequestParam String curso, @RequestParam int anoLectivo) {
        return Map.of("fasesActualizadas", servico.aplicarAosProcessos(curso, anoLectivo));
    }

    @PutMapping("/fases/{faseId}")
    public ResponseEntity<Void> ajustarFase(
            @PathVariable Long faseId, @Valid @RequestBody AjustarPrazoFasePedido pedido) {
        servico.ajustarFase(faseId, pedido);
        return ResponseEntity.noContent().build();
    }

    /** Execucao manual da verificacao diaria, util para validacao e demonstracao. */
    @PostMapping("/verificar")
    public Map<String, Integer> verificarAgora() {
        return Map.of(
                "atrasosMarcados", verificador.marcarAtrasos(),
                "avisosEnviados", verificador.avisarPrazosProximos());
    }
}
