package mz.co.isctem.tcc.supervisao;

import java.util.List;
import lombok.RequiredArgsConstructor;
import mz.co.isctem.tcc.seguranca.ContextoSeguranca;
import mz.co.isctem.tcc.supervisao.dto.EstudanteResumo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Vista do supervisor sobre os seus orientandos (RF11).
 *
 * <p>Separado de {@code /api/supervisao}, que e area do Coordenador: aqui o docente ve apenas os
 * estudantes sob a sua orientacao (RNF06).
 */
@RestController
@RequestMapping("/api/orientacao")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DOCENTE')")
public class ControladorOrientacao {

    private final ServicoSupervisao servico;
    private final ContextoSeguranca contexto;

    @GetMapping("/meus-orientandos")
    public List<EstudanteResumo> meusOrientandos() {
        return servico.meusOrientandos(contexto.idUtilizadorActual());
    }
}
