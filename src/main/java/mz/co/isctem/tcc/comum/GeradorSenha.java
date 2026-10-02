package mz.co.isctem.tcc.comum;

import java.security.SecureRandom;

/**
 * Gera as senhas temporarias entregues pelo Administrador (RF03).
 *
 * <p>O alfabeto exclui caracteres ambiguos (0/O, 1/l/I) porque estas senhas sao normalmente
 * comunicadas ao utilizador em papel ou verbalmente.
 */
public final class GeradorSenha {

    private static final String MAIUSCULAS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String MINUSCULAS = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITOS = "23456789";
    private static final String SIMBOLOS = "@#$%&*!";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private GeradorSenha() {}

    public static String gerar() {
        return gerar(12);
    }

    public static String gerar(int comprimento) {
        if (comprimento < 8) {
            throw new IllegalArgumentException("A senha temporaria tem de ter pelo menos 8 caracteres.");
        }
        String alfabeto = MAIUSCULAS + MINUSCULAS + DIGITOS + SIMBOLOS;
        StringBuilder senha = new StringBuilder(comprimento);
        // Garante pelo menos um caracter de cada classe.
        senha.append(sortear(MAIUSCULAS));
        senha.append(sortear(MINUSCULAS));
        senha.append(sortear(DIGITOS));
        senha.append(sortear(SIMBOLOS));
        for (int i = senha.length(); i < comprimento; i++) {
            senha.append(sortear(alfabeto));
        }
        return baralhar(senha);
    }

    private static char sortear(String origem) {
        return origem.charAt(ALEATORIO.nextInt(origem.length()));
    }

    private static String baralhar(StringBuilder texto) {
        char[] caracteres = texto.toString().toCharArray();
        for (int i = caracteres.length - 1; i > 0; i--) {
            int j = ALEATORIO.nextInt(i + 1);
            char temporario = caracteres[i];
            caracteres[i] = caracteres[j];
            caracteres[j] = temporario;
        }
        return new String(caracteres);
    }
}
