package mz.co.isctem.tcc.supervisao.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * RF04: atribuicao ou substituicao de supervisor pelo Coordenador.
 *
 * <p>O motivo e obrigatorio quando se substitui um supervisor ja atribuido, para que a alteracao
 * fique justificada no historico (RNF04).
 */
public record AtribuirSupervisorPedido(
        @NotNull(message = "O estudante e obrigatorio.") Long estudanteId,
        @NotNull(message = "O supervisor e obrigatorio.") Long docenteId,
        @Size(max = 500, message = "O motivo nao pode exceder 500 caracteres.") String motivo) {}
