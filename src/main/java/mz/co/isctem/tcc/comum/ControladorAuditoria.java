package mz.co.isctem.tcc.comum;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import mz.co.isctem.tcc.dominio.repositorio.AuditoriaRepositorio;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * RNF04: consulta do registo de auditoria — quem aprovou, rejeitou ou alterou o que, e quando.
 *
 * <p>Somente leitura: os registos nunca sao editaveis nem removiveis pela aplicacao.
 */
@RestController
@RequestMapping("/api/auditoria")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'COORDENADOR')")
public class ControladorAuditoria {

    private final AuditoriaRepositorio repositorio;

    public record RegistoAuditoria(
            Long id,
            String actor,
            String accao,
            String entidade,
            Long entidadeId,
            String detalhes,
            String enderecoIp,
            Instant criadoEm) {}

    @GetMapping
    public PaginaResposta<RegistoAuditoria> listar(
            @RequestParam(required = false) String entidade,
            @RequestParam(required = false) String accao,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamanho) {
        return PaginaResposta.de(
                repositorio.pesquisar(
                        vazioParaNulo(entidade), vazioParaNulo(accao), PageRequest.of(pagina, tamanho)),
                registo ->
                        new RegistoAuditoria(
                                registo.getId(),
                                registo.getActor(),
                                registo.getAccao(),
                                registo.getEntidade(),
                                registo.getEntidadeId(),
                                registo.getDetalhes(),
                                registo.getEnderecoIp(),
                                registo.getCriadoEm()));
    }

    private static String vazioParaNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
