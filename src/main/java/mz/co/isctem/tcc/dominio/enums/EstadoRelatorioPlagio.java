package mz.co.isctem.tcc.dominio.enums;

/** Resultado da tentativa de analise de similaridade (RF08). */
public enum EstadoRelatorioPlagio {
    /** Analise efectuada com sucesso. */
    CONCLUIDO,
    /** Ainda nao existem outros documentos na base de dados para comparacao. */
    SEM_CORPUS,
    /** Nao foi possivel extrair texto util do ficheiro (ex.: PDF digitalizado). */
    FALHOU
}
