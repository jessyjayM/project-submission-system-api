package mz.co.isctem.tcc.utilizador.dto;

import java.time.Instant;
import mz.co.isctem.tcc.dominio.enums.AreaCurso;
import mz.co.isctem.tcc.dominio.enums.EstadoUtilizador;
import mz.co.isctem.tcc.dominio.enums.PerfilUtilizador;

/** Vista de uma conta. Nunca inclui a senha, nem sequer o respectivo hash. */
public record UtilizadorResposta(
        Long id,
        String nome,
        PerfilUtilizador tipo,
        String email,
        String codigo,
        EstadoUtilizador estado,
        boolean deveAlterarSenha,
        Instant ultimoAcesso,
        Instant criadoEm,
        String curso,
        AreaCurso area,
        String departamento) {}
