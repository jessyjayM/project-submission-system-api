package mz.co.isctem.tcc.relatorio;

import lombok.RequiredArgsConstructor;
import mz.co.isctem.tcc.relatorio.dto.RelatoriosResposta;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** RF12: relatorios institucionais. Exclusivo do Coordenador (quadro da seccao 5.5). */
@RestController
@RequestMapping("/api/relatorios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COORDENADOR')")
public class ControladorRelatorios {

    private final ServicoRelatorios servico;

    @GetMapping
    public RelatoriosResposta gerar() {
        return servico.gerar();
    }
}
