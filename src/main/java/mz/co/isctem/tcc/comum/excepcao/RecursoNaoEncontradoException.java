package mz.co.isctem.tcc.comum.excepcao;

/** O recurso pedido nao existe. Traduzido para HTTP 404. */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }

    public static RecursoNaoEncontradoException de(String entidade, Object id) {
        return new RecursoNaoEncontradoException("%s nao encontrado(a): %s".formatted(entidade, id));
    }
}
