package mz.co.isctem.tcc.dominio.enums;

/**
 * Area do curso do estudante.
 *
 * <p>Determina se a Fase 5 (Prontidao do Sistema) faz parte do workflow: essa fase existe apenas
 * para cursos da area de Informatica.
 */
public enum AreaCurso {
    INFORMATICA,
    OUTRA;

    public boolean temFaseProntidaoSistema() {
        return this == INFORMATICA;
    }
}
