package mz.co.isctem.tcc.seguranca;

import mz.co.isctem.tcc.comum.excepcao.AcessoNegadoException;
import mz.co.isctem.tcc.dominio.enums.PerfilUtilizador;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** Acesso ao utilizador autenticado no pedido corrente. */
@Component
public class ContextoSeguranca {

    public UtilizadorAutenticado actual() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !(autenticacao.getPrincipal() instanceof Jwt jwt)) {
            throw new AcessoNegadoException("Pedido sem utilizador autenticado.");
        }
        return new UtilizadorAutenticado(
                Long.valueOf(jwt.getSubject()),
                jwt.getClaimAsString(Reivindicacoes.NOME),
                jwt.getClaimAsString(Reivindicacoes.CREDENCIAL),
                PerfilUtilizador.valueOf(jwt.getClaimAsString(Reivindicacoes.PERFIL)),
                Boolean.TRUE.equals(jwt.getClaim(Reivindicacoes.DEVE_ALTERAR_SENHA)));
    }

    public Long idUtilizadorActual() {
        return actual().id();
    }
}
