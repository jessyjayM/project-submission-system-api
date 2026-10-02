package mz.co.isctem.tcc.workflow.dto;

import java.time.LocalDate;
import java.util.List;
import mz.co.isctem.tcc.dominio.enums.AreaCurso;
import mz.co.isctem.tcc.dominio.enums.EstadoTcc;

/** Processo completo de TCC, tal como apresentado no ecra de workflow (RF05). */
public record TccResposta(
        Long id,
        Long estudanteId,
        String estudanteNome,
        String estudanteCodigo,
        String curso,
        AreaCurso area,
        String titulo,
        int faseActual,
        int ultimaFase,
        EstadoTcc estadoGeral,
        int percentagemConclusao,
        Long supervisorId,
        String supervisorNome,
        LocalDate dataInicio,
        LocalDate dataFimPrevista,
        boolean aptoParaDefesa,
        List<FaseResposta> fases) {}
