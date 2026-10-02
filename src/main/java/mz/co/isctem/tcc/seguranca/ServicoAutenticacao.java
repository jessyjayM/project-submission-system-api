package mz.co.isctem.tcc.seguranca;

import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mz.co.isctem.tcc.comum.ServicoAuditoria;
import mz.co.isctem.tcc.dominio.modelo.RefreshToken;
import mz.co.isctem.tcc.dominio.modelo.Utilizador;
import mz.co.isctem.tcc.dominio.repositorio.RefreshTokenRepositorio;
import mz.co.isctem.tcc.dominio.repositorio.UtilizadorRepositorio;
import mz.co.isctem.tcc.seguranca.dto.LoginPedido;
import mz.co.isctem.tcc.seguranca.dto.SessaoResposta;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF02: autenticacao, renovacao e termino de sessao. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServicoAutenticacao {

    private final UtilizadorRepositorio utilizadores;
    private final RefreshTokenRepositorio refreshTokens;
    private final PasswordEncoder codificador;
    private final ServicoTokens tokens;
    private final ServicoTentativasLogin tentativas;
    private final ServicoAuditoria auditoria;

    /**
     * Valida as credenciais e emite a sessao.
     *
     * <p>Deliberadamente sem {@code @Transactional}: o registo das tentativas falhadas tem de
     * sobreviver a excepcao lancada a seguir, pelo que corre em transaccao propria.
     */
    public SessaoResposta autenticar(LoginPedido pedido) {
        Utilizador utilizador =
                utilizadores
                        .findByCredencial(pedido.credencial().trim())
                        .orElseThrow(() -> new BadCredentialsException("Credenciais invalidas."));

        if (utilizador.bloqueado()) {
            long minutos =
                    Math.max(1, Duration.between(Instant.now(), utilizador.getBloqueadoAte()).toMinutes());
            throw new LockedException(
                    "Conta temporariamente bloqueada por tentativas falhadas. Tente novamente dentro de %d minuto(s)."
                            .formatted(minutos));
        }
        if (!utilizador.activo()) {
            throw new DisabledException("Conta desactivada. Contacte o Administrador do sistema.");
        }
        if (!codificador.matches(pedido.senha(), utilizador.getSenhaHash())) {
            boolean bloqueou = tentativas.registarFalha(utilizador.getId());
            auditoria.registar(
                    utilizador, "LOGIN_FALHADO", "Utilizador", utilizador.getId(), "bloqueou=" + bloqueou);
            throw bloqueou
                    ? new LockedException(
                            "Conta bloqueada temporariamente apos varias tentativas falhadas.")
                    : new BadCredentialsException("Credenciais invalidas.");
        }

        tentativas.registarSucesso(utilizador.getId());
        auditoria.registar(utilizador, "LOGIN", "Utilizador", utilizador.getId(), null);
        return construirSessao(utilizador);
    }

    /** Troca um refresh token valido por uma sessao nova, rodando o refresh token (RF02). */
    @Transactional
    public SessaoResposta renovar(String refreshTokenBruto) {
        if (refreshTokenBruto == null || refreshTokenBruto.isBlank()) {
            throw new BadCredentialsException("Refresh token em falta.");
        }
        RefreshToken guardado =
                refreshTokens
                        .findByTokenHash(ServicoTokens.hash(refreshTokenBruto))
                        .orElseThrow(() -> new BadCredentialsException("Refresh token invalido."));

        if (!guardado.valido()) {
            throw new BadCredentialsException("Sessao expirada. Volte a autenticar-se.");
        }
        Utilizador utilizador = guardado.getUtilizador();
        if (!utilizador.activo()) {
            throw new DisabledException("Conta desactivada. Contacte o Administrador do sistema.");
        }

        // Rotacao: o token usado deixa de servir, evitando reutilizacao em caso de fuga.
        guardado.setRevogado(true);
        return construirSessao(utilizador);
    }

    @Transactional
    public void terminarSessao(String refreshTokenBruto) {
        if (refreshTokenBruto == null || refreshTokenBruto.isBlank()) {
            return;
        }
        refreshTokens
                .findByTokenHash(ServicoTokens.hash(refreshTokenBruto))
                .ifPresent(token -> token.setRevogado(true));
    }

    private SessaoResposta construirSessao(Utilizador utilizador) {
        return new SessaoResposta(
                tokens.emitirAccessToken(utilizador),
                tokens.emitirRefreshToken(utilizador),
                "Bearer",
                tokens.segundosDeValidadeDoAccessToken(),
                utilizador.getId(),
                utilizador.getNome(),
                utilizador.getTipo(),
                utilizador.credencialLogin(),
                utilizador.isDeveAlterarSenha());
    }
}
