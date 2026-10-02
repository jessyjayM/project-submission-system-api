package mz.co.isctem.tcc.supervisao.dto;

import java.time.LocalDate;
import mz.co.isctem.tcc.dominio.enums.AreaCurso;
import mz.co.isctem.tcc.dominio.enums.EstadoProcessoEstudante;

/** Linha da lista de estudantes do Coordenador e do Supervisor (RF04, RF11). */
public record EstudanteResumo(
        Long estudanteId,
        Long utilizadorId,
        String nome,
        String codigo,
        String curso,
        AreaCurso area,
        EstadoProcessoEstudante estadoProcesso,
        Long supervisorId,
        String supervisorNome,
        Long tccId,
        Integer faseActual,
        String faseNome,
        String estadoFase,
        boolean emAtraso,
        LocalDate dataLimiteFase,
        Long diasRestantes,
        int percentagemConclusao) {}
