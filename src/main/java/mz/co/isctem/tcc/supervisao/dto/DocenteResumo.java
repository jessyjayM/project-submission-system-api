package mz.co.isctem.tcc.supervisao.dto;

/** Docente disponivel para orientacao, com o numero de orientandos actuais (RF04, RF11). */
public record DocenteResumo(
        Long docenteId, Long utilizadorId, String nome, String email, String departamento, long numeroOrientandos) {}
