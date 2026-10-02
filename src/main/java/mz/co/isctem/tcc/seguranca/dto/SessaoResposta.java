package mz.co.isctem.tcc.seguranca.dto;

import mz.co.isctem.tcc.dominio.enums.PerfilUtilizador;

/** Resultado de um login ou de uma renovacao de sessao (RF02). */
public record SessaoResposta(
        String accessToken,
        String refreshToken,
        String tipo,
        long expiraEmSegundos,
        Long utilizadorId,
        String nome,
        PerfilUtilizador perfil,
        String credencial,
        /** RF03: quando verdadeiro, o frontend redirecciona para a alteracao de senha. */
        boolean deveAlterarSenha) {}
