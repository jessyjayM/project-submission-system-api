package mz.co.isctem.tcc.seguranca;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import mz.co.isctem.tcc.seguranca.dto.LoginPedido;
import mz.co.isctem.tcc.seguranca.dto.SessaoResposta;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** RF02: endpoints publicos de autenticacao. */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class ControladorAutenticacao {

    private final ServicoAutenticacao servico;

    public record RefreshPedido(@NotBlank(message = "O refresh token e obrigatorio.") String refreshToken) {}

    @PostMapping("/login")
    public SessaoResposta login(@Valid @RequestBody LoginPedido pedido) {
        return servico.autenticar(pedido);
    }

    @PostMapping("/refresh")
    public SessaoResposta refresh(@Valid @RequestBody RefreshPedido pedido) {
        return servico.renovar(pedido.refreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshPedido pedido) {
        servico.terminarSessao(pedido == null ? null : pedido.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
