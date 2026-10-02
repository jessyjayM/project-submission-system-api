package mz.co.isctem.tcc.seguranca.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * RF02: autenticacao por login e senha.
 *
 * <p>O campo {@code credencial} aceita o codigo do estudante ou o email institucional do
 * docente/coordenador/administrador.
 */
public record LoginPedido(
        @NotBlank(message = "Indique o codigo de estudante ou o email institucional.") String credencial,
        @NotBlank(message = "A senha e obrigatoria.") String senha) {}
