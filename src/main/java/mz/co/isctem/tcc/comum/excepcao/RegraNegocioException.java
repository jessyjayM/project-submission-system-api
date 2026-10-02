package mz.co.isctem.tcc.comum.excepcao;

/** Uma regra de negocio impede a operacao. Traduzido para HTTP 409. */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
