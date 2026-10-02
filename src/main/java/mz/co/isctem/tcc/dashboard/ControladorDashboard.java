package mz.co.isctem.tcc.dashboard;

import lombok.RequiredArgsConstructor;
import mz.co.isctem.tcc.dashboard.dto.DashboardCoordenador;
import mz.co.isctem.tcc.dashboard.dto.DashboardEstudante;
import mz.co.isctem.tcc.dashboard.dto.DashboardSupervisor;
import mz.co.isctem.tcc.seguranca.ContextoSeguranca;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** RF11: dashboards por perfil. Cada rota so e acessivel ao perfil correspondente. */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class ControladorDashboard {

    private final ServicoDashboard servico;
    private final ContextoSeguranca contexto;

    @GetMapping("/coordenador")
    @PreAuthorize("hasRole('COORDENADOR')")
    public DashboardCoordenador coordenador() {
        return servico.doCoordenador();
    }

    @GetMapping("/supervisor")
    @PreAuthorize("hasRole('DOCENTE')")
    public DashboardSupervisor supervisor() {
        return servico.doSupervisor(contexto.idUtilizadorActual());
    }

    @GetMapping("/estudante")
    @PreAuthorize("hasRole('ESTUDANTE')")
    public DashboardEstudante estudante() {
        return servico.doEstudante(contexto.idUtilizadorActual());
    }
}
