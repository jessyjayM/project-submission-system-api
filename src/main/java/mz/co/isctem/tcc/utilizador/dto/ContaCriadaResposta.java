package mz.co.isctem.tcc.utilizador.dto;

/**
 * Resposta a criacao de conta ou a redefinicao de senha.
 *
 * <p>A senha temporaria e devolvida uma unica vez, no momento da operacao, para que o Administrador
 * a possa comunicar ao utilizador. Nao volta a estar acessivel por nenhum endpoint.
 */
public record ContaCriadaResposta(UtilizadorResposta utilizador, String senhaTemporaria) {}
