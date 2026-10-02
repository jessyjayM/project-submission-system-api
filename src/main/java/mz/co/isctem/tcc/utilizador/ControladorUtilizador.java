package mz.co.isctem.tcc.utilizador;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mz.co.isctem.tcc.comum.PaginaResposta;
import mz.co.isctem.tcc.dominio.enums.EstadoUtilizador;
import mz.co.isctem.tcc.dominio.enums.PerfilUtilizador;
import mz.co.isctem.tcc.utilizador.dto.ActualizarUtilizadorPedido;
import mz.co.isctem.tcc.utilizador.dto.ContaCriadaResposta;
import mz.co.isctem.tcc.utilizador.dto.CriarUtilizadorPedido;
import mz.co.isctem.tcc.utilizador.dto.UtilizadorResposta;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * RF01: gestao de utilizadores.
 *
 * <p>Toda a area e exclusiva do Administrador, conforme o quadro de permissoes da seccao 5.5.
 */
@RestController
@RequestMapping("/api/utilizadores")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ControladorUtilizador {

    private final ServicoUtilizador servico;

    @GetMapping
    public PaginaResposta<UtilizadorResposta> listar(
            @RequestParam(required = false) String termo,
            @RequestParam(required = false) PerfilUtilizador tipo,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return servico.listar(termo, tipo, PageRequest.of(pagina, tamanho, Sort.by("nome")));
    }

    @GetMapping("/{id}")
    public UtilizadorResposta porId(@PathVariable Long id) {
        return servico.porId(id);
    }

    @PostMapping
    public ResponseEntity<ContaCriadaResposta> criar(@Valid @RequestBody CriarUtilizadorPedido pedido) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servico.criar(pedido));
    }

    @PutMapping("/{id}")
    public UtilizadorResposta actualizar(
            @PathVariable Long id, @Valid @RequestBody ActualizarUtilizadorPedido pedido) {
        return servico.actualizar(id, pedido);
    }

    @PostMapping("/{id}/activar")
    public UtilizadorResposta activar(@PathVariable Long id) {
        return servico.alterarEstado(id, EstadoUtilizador.ACTIVO);
    }

    @PostMapping("/{id}/desactivar")
    public UtilizadorResposta desactivar(@PathVariable Long id) {
        return servico.alterarEstado(id, EstadoUtilizador.INACTIVO);
    }

    /** RF03: unica via de recuperacao de senha desta versao do sistema. */
    @PostMapping("/{id}/redefinir-senha")
    public ContaCriadaResposta redefinirSenha(@PathVariable Long id) {
        return servico.redefinirSenha(id);
    }
}
