package mz.co.isctem.tcc.seguranca;

import mz.co.isctem.tcc.dominio.enums.PerfilUtilizador;

/** Identidade do utilizador do pedido corrente, extraida do access token. */
public record UtilizadorAutenticado(
        Long id, String nome, String credencial, PerfilUtilizador perfil, boolean deveAlterarSenha) {

    public boolean admin() {
        return perfil == PerfilUtilizador.ADMIN;
    }

    public boolean coordenador() {
        return perfil == PerfilUtilizador.COORDENADOR;
    }

    public boolean docente() {
        return perfil == PerfilUtilizador.DOCENTE;
    }

    public boolean estudante() {
        return perfil == PerfilUtilizador.ESTUDANTE;
    }
}
