package mz.co.isctem.tcc.dominio.enums;

/**
 * Perfis suportados pela plataforma (RF01).
 *
 * <p>O documento identifica tres perfis principais (Estudante, Docente/Supervisor e Coordenador)
 * mais o Administrador, que e o unico com permissoes de gestao de contas.
 */
public enum PerfilUtilizador {
    ADMIN,
    COORDENADOR,
    DOCENTE,
    ESTUDANTE;

    /** Nome da authority usada pelo Spring Security. */
    public String authority() {
        return "ROLE_" + name();
    }

    /** RF02: estudantes autenticam-se pelo codigo, os restantes pelo email institucional. */
    public boolean autenticaPorCodigo() {
        return this == ESTUDANTE;
    }
}
