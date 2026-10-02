package mz.co.isctem.tcc.seguranca;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * RF03: enquanto o utilizador nao trocar a senha temporaria, qualquer pedido a outra funcionalidade
 * e bloqueado.
 *
 * <p>Criterio de aceitacao: "quando tenta aceder a qualquer pagina do sistema, entao deve ser
 * redireccionado obrigatoriamente para a pagina de alteracao de senha antes de continuar". O
 * backend devolve 403 com o codigo {@code SENHA_ALTERACAO_OBRIGATORIA} e o frontend faz o
 * redireccionamento.
 */
@Component
public class FiltroAlteracaoSenhaObrigatoria extends OncePerRequestFilter {

    /** Unicos caminhos acessiveis enquanto a senha temporaria nao for trocada. */
    private static final List<String> PERMITIDOS =
            List.of("/api/auth/", "/api/perfil/eu", "/api/perfil/senha", "/actuator/health");

    @Override
    protected void doFilterInternal(
            HttpServletRequest pedido, HttpServletResponse resposta, FilterChain cadeia)
            throws ServletException, IOException {

        if (deveAlterarSenha() && !caminhoPermitido(pedido.getRequestURI())) {
            resposta.setStatus(HttpStatus.FORBIDDEN.value());
            resposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
            resposta.setCharacterEncoding("UTF-8");
            resposta
                    .getWriter()
                    .write(
                            """
                            {"codigo":"SENHA_ALTERACAO_OBRIGATORIA",\
                            "mensagem":"Tem de alterar a senha antes de aceder ao sistema."}""");
            return;
        }
        cadeia.doFilter(pedido, resposta);
    }

    private boolean deveAlterarSenha() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        return autenticacao != null
                && autenticacao.getPrincipal() instanceof Jwt jwt
                && Boolean.TRUE.equals(jwt.getClaim(Reivindicacoes.DEVE_ALTERAR_SENHA));
    }

    private boolean caminhoPermitido(String caminho) {
        return PERMITIDOS.stream().anyMatch(caminho::startsWith);
    }
}
