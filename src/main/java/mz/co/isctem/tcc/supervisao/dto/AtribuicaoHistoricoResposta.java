package mz.co.isctem.tcc.supervisao.dto;

import java.time.Instant;

/** Entrada do historico de supervisores de um estudante (RF04, RNF04). */
public record AtribuicaoHistoricoResposta(
        Long id,
        Long docenteId,
        String docenteNome,
        String atribuidoPorNome,
        Instant inicio,
        Instant fim,
        boolean activa,
        String motivo) {}
