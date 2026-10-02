package mz.co.isctem.tcc.seguranca;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mz.co.isctem.tcc.config.PropriedadesIsctem;
import mz.co.isctem.tcc.dominio.modelo.Utilizador;
import mz.co.isctem.tcc.dominio.repositorio.UtilizadorRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF02: limita as tentativas de login falhadas consecutivas antes de bloquear temporariamente a
 * conta (5 por defeito).
 *
 * <p>Bean separado e com {@code REQUIRES_NEW} para que o contador seja gravado mesmo quando a
 * autenticacao termina em excepcao — caso contrario o rollback apagaria o registo da tentativa e o
 * bloqueio nunca aconteceria.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServicoTentativasLogin {

    private final UtilizadorRepositorio utilizadores;
    private final PropriedadesIsctem propriedades;

    /** @return true se esta falha esgotou as tentativas e bloqueou a conta. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean registarFalha(Long utilizadorId) {
        Utilizador utilizador = utilizadores.findById(utilizadorId).orElse(null);
        if (utilizador == null) {
            return false;
        }
        int maximo = propriedades.security().maxFailedAttempts();
        utilizador.setTentativasFalhadas(utilizador.getTentativasFalhadas() + 1);

        if (utilizador.getTentativasFalhadas() >= maximo) {
            utilizador.setBloqueadoAte(Instant.now().plus(propriedades.security().lockDuration()));
            utilizador.setTentativasFalhadas(0);
            log.warn(
                    "Conta {} bloqueada temporariamente apos {} tentativas falhadas.",
                    utilizador.credencialLogin(),
                    maximo);
            return true;
        }
        return false;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registarSucesso(Long utilizadorId) {
        utilizadores.findById(utilizadorId).ifPresent(Utilizador::registarAcessoValido);
    }

    public int tentativasRestantes(Utilizador utilizador) {
        return Math.max(0, propriedades.security().maxFailedAttempts() - utilizador.getTentativasFalhadas());
    }
}
