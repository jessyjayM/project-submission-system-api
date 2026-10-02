package mz.co.isctem.tcc.comum;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Corpo uniforme das respostas de erro da API. */
public record RespostaErro(
        Instant momento,
        int estado,
        String erro,
        String mensagem,
        String caminho,
        Map<String, List<String>> camposInvalidos) {

    public static RespostaErro de(int estado, String erro, String mensagem, String caminho) {
        return new RespostaErro(Instant.now(), estado, erro, mensagem, caminho, Map.of());
    }
}
