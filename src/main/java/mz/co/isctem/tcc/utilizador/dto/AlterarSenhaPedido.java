package mz.co.isctem.tcc.utilizador.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Alteracao de senha pelo proprio utilizador (RF03).
 *
 * <p>Criterio de aceitacao: "Dado que a nova senha tem menos de 8 caracteres, quando o utilizador
 * tenta submeter o formulario, entao o sistema deve rejeitar a alteracao e indicar o motivo."
 */
public record AlterarSenhaPedido(
        @NotBlank(message = "A senha actual e obrigatoria.") String senhaActual,
        @NotBlank(message = "A nova senha e obrigatoria.")
                @Size(min = 8, max = 72, message = "A nova senha deve ter no minimo 8 caracteres.")
                String novaSenha,
        @NotBlank(message = "A confirmacao da nova senha e obrigatoria.") String confirmarNovaSenha) {

    public boolean confirmacaoCoincide() {
        return novaSenha != null && novaSenha.equals(confirmarNovaSenha);
    }
}
