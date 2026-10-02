package mz.co.isctem.tcc.utilizador;

import lombok.RequiredArgsConstructor;
import mz.co.isctem.tcc.comum.ServicoAuditoria;
import mz.co.isctem.tcc.comum.excepcao.RecursoNaoEncontradoException;
import mz.co.isctem.tcc.comum.excepcao.RegraNegocioException;
import mz.co.isctem.tcc.config.PropriedadesIsctem;
import mz.co.isctem.tcc.dominio.modelo.Utilizador;
import mz.co.isctem.tcc.dominio.repositorio.UtilizadorRepositorio;
import mz.co.isctem.tcc.seguranca.ServicoTokens;
import mz.co.isctem.tcc.utilizador.dto.AlterarSenhaPedido;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF03: gestao da propria senha, disponivel a todos os perfis. */
@Service
@RequiredArgsConstructor
public class ServicoPerfil {

    private final UtilizadorRepositorio utilizadores;
    private final PasswordEncoder codificador;
    private final ServicoTokens tokens;
    private final ServicoAuditoria auditoria;
    private final PropriedadesIsctem propriedades;

    /**
     * Altera a senha do proprio utilizador.
     *
     * <p>Aplica-se tanto a troca obrigatoria no primeiro acesso como a alteracao voluntaria a partir
     * do perfil. Todas as sessoes activas sao invalidadas: o access token em circulacao ainda
     * transporta {@code deve_alterar_senha = true} e deixaria de reflectir a realidade.
     */
    @Transactional
    public void alterarSenha(Long utilizadorId, AlterarSenhaPedido pedido) {
        Utilizador utilizador =
                utilizadores
                        .findById(utilizadorId)
                        .orElseThrow(() -> RecursoNaoEncontradoException.de("Utilizador", utilizadorId));

        if (!codificador.matches(pedido.senhaActual(), utilizador.getSenhaHash())) {
            throw new RegraNegocioException("A senha actual esta incorrecta.");
        }
        if (!pedido.confirmacaoCoincide()) {
            throw new RegraNegocioException("A nova senha e a confirmacao nao coincidem.");
        }
        int minimo = propriedades.security().senhaTamanhoMinimo();
        if (pedido.novaSenha().length() < minimo) {
            throw new RegraNegocioException(
                    "A nova senha deve ter no minimo %d caracteres.".formatted(minimo));
        }
        if (codificador.matches(pedido.novaSenha(), utilizador.getSenhaHash())) {
            throw new RegraNegocioException("A nova senha tem de ser diferente da senha actual.");
        }

        utilizador.setSenhaHash(codificador.encode(pedido.novaSenha()));
        utilizador.setDeveAlterarSenha(false);
        tokens.revogarTodosDe(utilizadorId);

        auditoria.registar(utilizador, "ALTERAR_SENHA", "Utilizador", utilizadorId, null);
    }
}
