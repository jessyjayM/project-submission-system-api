package mz.co.isctem.tcc.supervisao;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import mz.co.isctem.tcc.comum.PaginaResposta;
import mz.co.isctem.tcc.supervisao.dto.AtribuicaoHistoricoResposta;
import mz.co.isctem.tcc.supervisao.dto.AtribuirSupervisorPedido;
import mz.co.isctem.tcc.supervisao.dto.DocenteResumo;
import mz.co.isctem.tcc.supervisao.dto.EstudanteResumo;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * RF04: gestao de supervisores.
 *
 * <p>Area exclusiva do Coordenador, conforme o quadro de permissoes da seccao 5.5.
 */
@RestController
@RequestMapping("/api/supervisao")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COORDENADOR')")
public class ControladorSupervisao {

    private final ServicoSupervisao servico;

    @GetMapping("/estudantes")
    public PaginaResposta<EstudanteResumo> estudantes(
            @RequestParam(required = false) String termo,
            @RequestParam(required = false) Long supervisorId,
            @RequestParam(defaultValue = "false") boolean semSupervisor,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return servico.listarEstudantes(
                termo, supervisorId, semSupervisor, PageRequest.of(pagina, tamanho, Sort.by("utilizador.nome")));
    }

    @GetMapping("/docentes")
    public List<DocenteResumo> docentes() {
        return servico.listarDocentes();
    }

    /** Atribui ou substitui o supervisor. A primeira atribuicao arranca o workflow do TCC. */
    @PostMapping("/atribuicoes")
    public EstudanteResumo atribuir(@Valid @RequestBody AtribuirSupervisorPedido pedido) {
        return servico.atribuir(pedido);
    }

    @GetMapping("/estudantes/{estudanteId}/historico")
    public List<AtribuicaoHistoricoResposta> historico(@PathVariable Long estudanteId) {
        return servico.historico(estudanteId);
    }
}
