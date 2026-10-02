package mz.co.isctem.tcc.comum.excepcao;

/**
 * O utilizador esta autenticado mas nao tem permissao sobre este recurso (RNF06: isolamento de
 * dados entre orientandos de supervisores diferentes). Traduzido para HTTP 403.
 */
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
