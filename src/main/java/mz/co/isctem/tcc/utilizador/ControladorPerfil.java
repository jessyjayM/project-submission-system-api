package mz.co.isctem.tcc.utilizador;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mz.co.isctem.tcc.seguranca.ContextoSeguranca;
import mz.co.isctem.tcc.utilizador.dto.AlterarSenhaPedido;
import mz.co.isctem.tcc.utilizador.dto.UtilizadorResposta;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RF03: perfil do proprio utilizador.
 *
 * <p>Estes dois endpoints continuam acessiveis mesmo quando a troca de senha e obrigatoria — ver
 * {@link mz.co.isctem.tcc.seguranca.FiltroAlteracaoSenhaObrigatoria}.
 */
@RestController
@RequestMapping("/api/perfil")
@RequiredArgsConstructor
public class ControladorPerfil {

    private final ServicoPerfil servicoPerfil;
    private final ServicoUtilizador servicoUtilizador;
    private final ContextoSeguranca contexto;

    @GetMapping("/eu")
    public UtilizadorResposta eu() {
        return servicoUtilizador.porId(contexto.idUtilizadorActual());
    }

    @PutMapping("/senha")
    public ResponseEntity<Void> alterarSenha(@Valid @RequestBody AlterarSenhaPedido pedido) {
        servicoPerfil.alterarSenha(contexto.idUtilizadorActual(), pedido);
        return ResponseEntity.noContent().build();
    }
}
