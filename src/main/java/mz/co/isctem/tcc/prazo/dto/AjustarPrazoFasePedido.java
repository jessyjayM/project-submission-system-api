package mz.co.isctem.tcc.prazo.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * RF10: ajuste do prazo de uma fase de um estudante concreto, sobrepondo-se ao plano do curso.
 *
 * <p>Serve para os casos individuais (prorrogacoes justificadas) sem alterar o calendario dos
 * restantes estudantes.
 */
public record AjustarPrazoFasePedido(
        LocalDate dataInicio, @NotNull(message = "A data limite e obrigatoria.") LocalDate dataLimite) {}
