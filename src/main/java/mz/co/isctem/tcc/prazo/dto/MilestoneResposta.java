package mz.co.isctem.tcc.prazo.dto;

import java.time.LocalDate;

/** Marco/prazo definido pelo Coordenador (RF10). */
public record MilestoneResposta(
        Long id,
        String curso,
        int anoLectivo,
        int numeroFase,
        String nome,
        LocalDate dataInicio,
        LocalDate dataLimite,
        long duracaoDias) {}
