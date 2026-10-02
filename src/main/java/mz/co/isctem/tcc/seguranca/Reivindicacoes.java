package mz.co.isctem.tcc.seguranca;

/** Nomes das reivindicacoes (claims) transportadas no access token. */
public final class Reivindicacoes {

    public static final String NOME = "nome";
    public static final String PERFIL = "perfil";
    public static final String CREDENCIAL = "credencial";
    /** RF03: enquanto verdadeiro, o utilizador so pode aceder a alteracao de senha. */
    public static final String DEVE_ALTERAR_SENHA = "deve_alterar_senha";

    private Reivindicacoes() {}
}
