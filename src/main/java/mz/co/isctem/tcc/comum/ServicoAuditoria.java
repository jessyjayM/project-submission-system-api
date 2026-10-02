package mz.co.isctem.tcc.comum;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import mz.co.isctem.tcc.dominio.modelo.Auditoria;
import mz.co.isctem.tcc.dominio.modelo.Utilizador;
import mz.co.isctem.tcc.dominio.repositorio.AuditoriaRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * RNF04: registo de auditoria das accoes relevantes.
 *
 * <p>Corre em transaccao propria ({@code REQUIRES_NEW}) para que o registo persista mesmo que a
 * operacao de negocio venha a falhar depois.
 */
@Service
@RequiredArgsConstructor
public class ServicoAuditoria {

    private final AuditoriaRepositorio repositorio;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registar(Utilizador actor, String accao, String entidade, Long entidadeId, String detalhes) {
        repositorio.save(
                Auditoria.builder()
                        .utilizador(actor)
                        .actor(actor == null ? "sistema" : actor.getNome())
                        .accao(accao)
                        .entidade(entidade)
                        .entidadeId(entidadeId)
                        .detalhes(truncar(detalhes))
                        .enderecoIp(enderecoIp())
                        .build());
    }

    /** Variante para accoes automaticas do sistema (ex.: verificacao diaria de prazos). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registarDoSistema(String accao, String entidade, Long entidadeId, String detalhes) {
        registar(null, accao, entidade, entidadeId, detalhes);
    }

    private String truncar(String texto) {
        if (texto == null) {
            return null;
        }
        return texto.length() <= 2000 ? texto : texto.substring(0, 1997) + "...";
    }

    private String enderecoIp() {
        if (RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes atributos) {
            HttpServletRequest pedido = atributos.getRequest();
            String encaminhado = pedido.getHeader("X-Forwarded-For");
            return encaminhado != null ? encaminhado.split(",")[0].trim() : pedido.getRemoteAddr();
        }
        return null;
    }
}
