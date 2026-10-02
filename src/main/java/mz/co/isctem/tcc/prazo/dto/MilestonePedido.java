package mz.co.isctem.tcc.prazo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** RF10: definicao de data de inicio e data limite de uma fase, por curso e ano lectivo. */
public record MilestonePedido(
        @NotBlank(message = "O curso e obrigatorio.") @Size(max = 120) String curso,
        @NotNull(message = "O ano lectivo e obrigatorio.") @Min(2000) @Max(2100) Integer anoLectivo,
        @NotNull(message = "O numero da fase e obrigatorio.") @Min(1) @Max(5) Integer numeroFase,
        @NotNull(message = "A data de inicio e obrigatoria.") LocalDate dataInicio,
        @NotNull(message = "A data limite e obrigatoria.") LocalDate dataLimite) {

    public boolean intervaloValido() {
        return dataInicio != null && dataLimite != null && !dataLimite.isBefore(dataInicio);
    }
}
